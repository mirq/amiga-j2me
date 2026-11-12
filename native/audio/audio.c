/* amiga-j2me audio mixer, WAV reader and MIDI synthesiser (see audio.h). GPLv2. */
#include "audio.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>

#define MAX_SOUNDS   48
#define MAX_VOICES   24
#define BLOCK        64          /* frames between MIDI event checks */

enum { SND_FREE = 0, SND_PCM, SND_MIDI };

typedef struct { uint32_t tick; uint32_t seq; uint8_t status, d1, d2; uint32_t tempo; } MidiEvent;

typedef struct {
    int      active, chan, note, drum;
    uint32_t phase, step;
    int      wave;               /* 0 pulse 50%, 1 pulse 25%, 2 saw, 3 triangle, 4 noise */
    int32_t  env;                /* 16.16, 0..65536 */
    int      stage;              /* 0 attack, 1 decay, 2 sustain, 3 release */
    int32_t  attack, decay, sustain, release;
    int      vel;
    uint32_t lfsr;
    int32_t  sweep;              /* per-sample step change (kick drum) */
} Voice;

typedef struct {
    int       type, playing, loops, volume;
    /* PCM */
    int16_t  *pcm;               /* interleaved when stereo */
    int       frames, channels, rate;
    uint32_t  pos, frac, step;   /* frame position, 16.16 fraction and step */
    /* MIDI */
    MidiEvent *ev;
    int       nev, division;
    int       next;              /* next event index */
    uint32_t  tick;              /* current tick */
    uint64_t  tick_frac;         /* 32.32 ticks accumulated */
    uint64_t  ticks_per_frame;   /* 32.32 */
    uint32_t  total_ms, played_frames;
    uint8_t   prog[16], vol[16], expr[16];
    Voice     voice[MAX_VOICES];
} Sound;

static Sound s_snd[MAX_SOUNDS + 1];
static int   s_rate = 22050;
static uint32_t s_note_step[128];       /* phase step per sample for each MIDI note */
static int32_t  s_mixbuf[BLOCK * 2];

void aud_init(int rate)
{
    int n;
    s_rate = rate > 0 ? rate : 22050;
    for (n = 0; n < 128; n++) {
        double f = 440.0 * pow(2.0, (n - 69) / 12.0);
        s_note_step[n] = (uint32_t)(f * 4294967296.0 / s_rate);
    }
}

static Sound *get(int h) { return (h > 0 && h <= MAX_SOUNDS && s_snd[h].type != SND_FREE) ? &s_snd[h] : NULL; }

/* ---- WAV ---- */

static unsigned rd16(const uint8_t *p) { return p[0] | (p[1] << 8); }
static uint32_t rd32(const uint8_t *p) { return p[0] | (p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24); }

static int load_wav(Sound *s, const uint8_t *d, int len)
{
    int pos = 12, fmt = 0, channels = 0, rate = 0, bits = 0, i, n;
    const uint8_t *data = NULL; int dlen = 0;
    if (len < 44 || memcmp(d, "RIFF", 4) || memcmp(d + 8, "WAVE", 4)) return 0;
    while (pos + 8 <= len) {
        uint32_t size = rd32(d + pos + 4);
        if (size > (uint32_t)(len - pos - 8)) size = (uint32_t)(len - pos - 8);
        if (!memcmp(d + pos, "fmt ", 4) && size >= 16) {
            fmt = rd16(d + pos + 8); channels = rd16(d + pos + 10);
            rate = (int)rd32(d + pos + 12); bits = rd16(d + pos + 22);
        } else if (!memcmp(d + pos, "data", 4)) {
            data = d + pos + 8; dlen = (int)size;
        }
        pos += 8 + (int)size + (size & 1);
    }
    if (!data || fmt != 1 || (channels != 1 && channels != 2) || (bits != 8 && bits != 16) || rate <= 0) return 0;
    n = dlen / (bits / 8);
    s->pcm = (int16_t *)malloc((size_t)(n ? n : 1) * 2);
    if (!s->pcm) return 0;
    if (bits == 8) for (i = 0; i < n; i++) s->pcm[i] = (int16_t)((data[i] - 128) << 8);
    else for (i = 0; i < n; i++) s->pcm[i] = (int16_t)rd16(data + i * 2);
    s->channels = channels; s->rate = rate; s->frames = n / channels;
    s->step = (uint32_t)(((uint64_t)rate << 16) / (uint32_t)s_rate);
    s->total_ms = (uint32_t)((uint64_t)s->frames * 1000 / (uint32_t)rate);
    s->type = SND_PCM;
    return 1;
}

/* ---- Standard MIDI File ---- */

static uint32_t be32(const uint8_t *p) { return ((uint32_t)p[0] << 24) | (p[1] << 16) | (p[2] << 8) | p[3]; }

/* by tick, then in file order (seq): a total order, as qsort needs */
static int cmp_ev(const void *a, const void *b)
{
    const MidiEvent *x = (const MidiEvent *)a, *y = (const MidiEvent *)b;
    if (x->tick != y->tick) return x->tick < y->tick ? -1 : 1;
    if (x->seq != y->seq) return x->seq < y->seq ? -1 : 1;
    return 0;
}

static int add_ev(Sound *s, int *cap, uint32_t tick, int status, int d1, int d2, uint32_t tempo)
{
    MidiEvent *e;
    if (s->nev == *cap) {
        int nc = *cap ? *cap * 2 : 512;
        /* no realloc(NULL, ...): not every C library on these systems takes it */
        MidiEvent *n = (MidiEvent *)malloc((size_t)nc * sizeof(MidiEvent));
        if (n && s->ev) { memcpy(n, s->ev, (size_t)s->nev * sizeof(MidiEvent)); free(s->ev); }
        if (!n) return 0;
        s->ev = n; *cap = nc;
    }
    e = &s->ev[s->nev];
    e->seq = (uint32_t)s->nev++;
    e->tick = tick; e->status = (uint8_t)status; e->d1 = (uint8_t)d1; e->d2 = (uint8_t)d2; e->tempo = tempo;
    return 1;
}

static int load_midi(Sound *s, const uint8_t *d, int len)
{
    int pos, ntracks, t, cap = 0, i;
    uint32_t tempo = 500000, last_tick = 0;
    uint64_t us = 0;
    if (len < 14 || memcmp(d, "MThd", 4)) return 0;
    ntracks = (d[10] << 8) | d[11];
    s->division = (d[12] << 8) | d[13];
    if (s->division <= 0 || (s->division & 0x8000)) s->division = 96;   /* SMPTE time: not supported */
    pos = 8 + (int)be32(d + 4);
    for (t = 0; t < ntracks && pos + 8 <= len; t++) {
        int end, running = 0;
        uint32_t tick = 0;
        if (memcmp(d + pos, "MTrk", 4)) break;
        end = pos + 8 + (int)be32(d + pos + 4);
        if (end > len) end = len;
        pos += 8;
        while (pos < end) {
            uint32_t delta = 0; int b, status;
            do { b = d[pos++]; delta = (delta << 7) | (b & 0x7F); } while ((b & 0x80) && pos < end);
            tick += delta;
            if (pos >= end) break;
            status = d[pos];
            if (status & 0x80) { pos++; if (status < 0xF0) running = status; }
            else status = running;
            if (status == 0xFF) {                   /* meta */
                int type = d[pos++]; uint32_t mlen = 0;
                do { b = d[pos++]; mlen = (mlen << 7) | (b & 0x7F); } while ((b & 0x80) && pos < end);
                if (type == 0x51 && mlen == 3 && pos + 3 <= end)
                    add_ev(s, &cap, tick, 0xFF, 0, 0, ((uint32_t)d[pos] << 16) | (d[pos + 1] << 8) | d[pos + 2]);
                pos += (int)mlen;
                if (type == 0x2F) break;
            } else if (status == 0xF0 || status == 0xF7) {   /* sysex */
                uint32_t mlen = 0;
                do { b = d[pos++]; mlen = (mlen << 7) | (b & 0x7F); } while ((b & 0x80) && pos < end);
                pos += (int)mlen;
            } else if (status >= 0x80) {
                int hi = status & 0xF0, d1 = d[pos++] & 0x7F, d2 = 0;
                if (hi != 0xC0 && hi != 0xD0) d2 = d[pos++] & 0x7F;
                if (hi == 0x80 || hi == 0x90 || hi == 0xB0 || hi == 0xC0)
                    add_ev(s, &cap, tick, status, d1, d2, 0);
            } else break;                           /* garbage */
        }
        pos = end;
    }
    if (s->nev == 0) { free(s->ev); s->ev = NULL; return 0; }
    /* tracks are merged by tick, keeping their order */
    qsort(s->ev, (size_t)s->nev, sizeof(MidiEvent), cmp_ev);
    for (i = 0; i < s->nev; i++) {
        us += (uint64_t)(s->ev[i].tick - last_tick) * tempo / (uint32_t)s->division;
        last_tick = s->ev[i].tick;
        if (s->ev[i].status == 0xFF) tempo = s->ev[i].tempo;
    }
    s->total_ms = (uint32_t)(us / 1000) + 500;      /* + release tail */
    s->type = SND_MIDI;
    return 1;
}

static void midi_set_tempo(Sound *s, uint32_t tempo_us)
{
    /* ticks per output frame = division * 1e6 / (tempo * rate), as 32.32 */
    s->ticks_per_frame = (((uint64_t)s->division * 1000000u) << 32) / ((uint64_t)tempo_us * (uint32_t)s_rate);
}

static void midi_reset(Sound *s)
{
    int c;
    s->next = 0; s->tick = 0; s->tick_frac = 0; s->played_frames = 0;
    memset(s->voice, 0, sizeof(s->voice));
    for (c = 0; c < 16; c++) { s->prog[c] = 0; s->vol[c] = 100; s->expr[c] = 127; }
    midi_set_tempo(s, 500000);
}

/* envelope rates are 16.16 level change per sample for the given milliseconds */
static int32_t rate_ms(int ms) { int32_t r = (int32_t)(65536LL * 1000 / ((int64_t)(ms > 0 ? ms : 1) * s_rate)); return r > 0 ? r : 1; }

static void note_on(Sound *s, int chan, int note, int vel)
{
    Voice *v = NULL; int i, prog = s->prog[chan];
    int32_t quiet = 0x7FFFFFFF;
    for (i = 0; i < MAX_VOICES; i++) if (!s->voice[i].active) { v = &s->voice[i]; break; }
    if (!v) {                           /* steal the quietest */
        for (i = 0; i < MAX_VOICES; i++) if (s->voice[i].env < quiet) { quiet = s->voice[i].env; v = &s->voice[i]; }
    }
    memset(v, 0, sizeof(*v));
    v->active = 1; v->chan = chan; v->note = note; v->vel = vel; v->lfsr = 0xACE1u + (uint32_t)note * 77u;
    if (chan == 9) {                    /* percussion */
        v->drum = 1; v->attack = 65536; v->sustain = 0; v->release = rate_ms(40);
        if (note <= 36) {               /* kick: a falling triangle */
            v->wave = 3; v->step = s_note_step[40]; v->sweep = -(int32_t)(s_note_step[40] / (uint32_t)(s_rate / 8 + 1));
            v->decay = rate_ms(140);
        } else if (note == 38 || note == 40) { v->wave = 4; v->decay = rate_ms(110); v->step = 1; }     /* snare */
        else if (note == 42 || note == 44) { v->wave = 4; v->decay = rate_ms(35); v->step = 1; }        /* closed hat */
        else if (note == 46 || note == 49 || note == 51 || note == 57) { v->wave = 4; v->decay = rate_ms(260); v->step = 1; }
        else if (note >= 41 && note <= 50) { v->wave = 3; v->step = s_note_step[note]; v->decay = rate_ms(180); }  /* toms */
        else { v->wave = 4; v->decay = rate_ms(60); v->step = 1; }
        return;
    }
    v->step = s_note_step[note];
    if (prog < 8)       { v->wave = 3; v->attack = rate_ms(2);  v->decay = rate_ms(900);  v->sustain = 6000;  v->release = rate_ms(180); }  /* piano */
    else if (prog < 16) { v->wave = 3; v->attack = rate_ms(1);  v->decay = rate_ms(500);  v->sustain = 0;     v->release = rate_ms(200); }  /* chromatic perc. */
    else if (prog < 24) { v->wave = 0; v->attack = rate_ms(8);  v->decay = rate_ms(100);  v->sustain = 52000; v->release = rate_ms(60); }   /* organ */
    else if (prog < 32) { v->wave = 2; v->attack = rate_ms(2);  v->decay = rate_ms(700);  v->sustain = 5000;  v->release = rate_ms(120); }  /* guitar */
    else if (prog < 40) { v->wave = 3; v->attack = rate_ms(3);  v->decay = rate_ms(500);  v->sustain = 30000; v->release = rate_ms(80); }   /* bass */
    else if (prog < 56) { v->wave = 2; v->attack = rate_ms(60); v->decay = rate_ms(300);  v->sustain = 45000; v->release = rate_ms(200); }  /* strings, ensemble */
    else if (prog < 80) { v->wave = 1; v->attack = rate_ms(20); v->decay = rate_ms(200);  v->sustain = 45000; v->release = rate_ms(90); }   /* brass, reed, pipe */
    else if (prog < 88) { v->wave = prog & 1 ? 2 : 0; v->attack = rate_ms(4); v->decay = rate_ms(200); v->sustain = 48000; v->release = rate_ms(80); }  /* synth lead */
    else                { v->wave = 2; v->attack = rate_ms(40); v->decay = rate_ms(400);  v->sustain = 40000; v->release = rate_ms(250); }  /* pads and the rest */
}

static void note_off(Sound *s, int chan, int note)
{
    int i;
    for (i = 0; i < MAX_VOICES; i++) {
        Voice *v = &s->voice[i];
        if (v->active && !v->drum && v->chan == chan && v->note == note && v->stage < 3) v->stage = 3;
    }
}

static void midi_event(Sound *s, const MidiEvent *e)
{
    int chan = e->status & 0x0F, i;
    switch (e->status & 0xF0) {
    case 0x90: if (e->d2) { note_on(s, chan, e->d1, e->d2); break; }   /* velocity 0 = note off */
               /* fall through */
    case 0x80: note_off(s, chan, e->d1); break;
    case 0xB0:
        if (e->d1 == 7) s->vol[chan] = e->d2;
        else if (e->d1 == 11) s->expr[chan] = e->d2;
        else if (e->d1 == 120 || e->d1 == 123)
            for (i = 0; i < MAX_VOICES; i++) if (s->voice[i].chan == chan && s->voice[i].stage < 3) s->voice[i].stage = 3;
        break;
    case 0xC0: s->prog[chan] = e->d1; break;
    case 0xF0: if (e->status == 0xFF) midi_set_tempo(s, e->tempo ? e->tempo : 500000); break;
    default: break;
    }
}

/* renders n (<= BLOCK) mono frames of all voices into buf (added) */
static int render_voices(Sound *s, int32_t *buf, int n)
{
    int i, k, any = 0;
    for (i = 0; i < MAX_VOICES; i++) {
        Voice *v = &s->voice[i];
        int32_t gain;
        if (!v->active) continue;
        any = 1;
        gain = (v->vel * s->vol[v->chan] * s->expr[v->chan]) >> 11;     /* 0..~1000 */
        for (k = 0; k < n; k++) {
            int32_t smp;
            switch (v->stage) {
            case 0: v->env += v->attack; if (v->env >= 65536) { v->env = 65536; v->stage = 1; } break;
            case 1: v->env -= v->decay; if (v->env <= v->sustain) { v->env = v->sustain; v->stage = 2; if (v->sustain == 0) v->active = 0; } break;
            case 2: break;
            default: v->env -= v->release; if (v->env <= 0) { v->env = 0; v->active = 0; } break;
            }
            if (!v->active) break;
            switch (v->wave) {
            case 0: smp = (v->phase & 0x80000000u) ? -6000 : 6000; break;
            case 1: smp = (v->phase < 0x40000000u) ? 6000 : -2000; break;
            case 2: smp = (int32_t)(v->phase >> 20) - 2048; smp *= 3; break;
            case 3: { int32_t t = (int32_t)(v->phase >> 19) & 0x1FFF; smp = (t < 0x1000 ? t : 0x1FFF - t) - 0x800; smp *= 5; } break;
            default: v->lfsr = (v->lfsr >> 1) ^ (-(int32_t)(v->lfsr & 1u) & 0xB400u); smp = (int32_t)(v->lfsr & 0x1FFF) - 0x1000; break;
            }
            v->phase += v->step;
            if (v->sweep) { int32_t ns = (int32_t)v->step + v->sweep; v->step = ns > 0x100000 ? (uint32_t)ns : 0x100000u; }
            buf[k] += (((smp * (v->env >> 4)) >> 12) * gain) >> 10;
        }
    }
    return any;
}

/* ---- public ---- */

int aud_create(const uint8_t *data, int len)
{
    int h;
    for (h = 1; h <= MAX_SOUNDS; h++) if (s_snd[h].type == SND_FREE) break;
    if (h > MAX_SOUNDS) return 0;
    memset(&s_snd[h], 0, sizeof(Sound));
    s_snd[h].volume = 100;
    if (load_wav(&s_snd[h], data, len)) return h;
    if (load_midi(&s_snd[h], data, len)) { midi_reset(&s_snd[h]); return h; }
    return 0;
}

void aud_destroy(int h)
{
    Sound *s = get(h);
    if (!s) return;
    free(s->pcm); free(s->ev);
    memset(s, 0, sizeof(*s));
}

void aud_rewind(int h)
{
    Sound *s = get(h);
    if (!s) return;
    if (s->type == SND_PCM) { s->pos = 0; s->frac = 0; } else midi_reset(s);
}

void aud_play(int h, int loops, int volume)
{
    Sound *s = get(h);
    if (!s) return;
    s->loops = loops; s->volume = volume < 0 ? 0 : (volume > 100 ? 100 : volume);
    if (s->type == SND_PCM && s->pos >= (uint32_t)s->frames) { s->pos = 0; s->frac = 0; }
    if (s->type == SND_MIDI && s->next >= s->nev) midi_reset(s);
    s->playing = 1;
}

void aud_stop(int h)
{
    Sound *s = get(h);
    if (!s) return;
    s->playing = 0;
    if (s->type == SND_MIDI) memset(s->voice, 0, sizeof(s->voice));
}

void aud_set_volume(int h, int volume) { Sound *s = get(h); if (s) s->volume = volume < 0 ? 0 : (volume > 100 ? 100 : volume); }
int  aud_is_playing(int h) { Sound *s = get(h); return s ? s->playing : 0; }
int  aud_duration_ms(int h) { Sound *s = get(h); return s ? (int)s->total_ms : -1; }

int aud_position_ms(int h)
{
    Sound *s = get(h);
    if (!s) return 0;
    if (s->type == SND_PCM) return (int)((uint64_t)s->pos * 1000 / (uint32_t)s->rate);
    return (int)((uint64_t)s->played_frames * 1000 / (uint32_t)s_rate);
}

static void finished(Sound *s)
{
    if (s->loops < 0 || --s->loops > 0) {
        if (s->type == SND_PCM) { s->pos = 0; s->frac = 0; } else midi_reset(s);
    } else s->playing = 0;
}

void aud_mix(int16_t *out, int frames)
{
    while (frames > 0) {
        int n = frames > BLOCK ? BLOCK : frames, h, k;
        memset(s_mixbuf, 0, sizeof(int32_t) * (size_t)n * 2);
        for (h = 1; h <= MAX_SOUNDS; h++) {
            Sound *s = &s_snd[h];
            if (s->type == SND_FREE || !s->playing) continue;
            if (s->type == SND_PCM) {
                int vol = (s->volume * 256) / 100;
                for (k = 0; k < n; k++) {
                    int32_t l, r;
                    if (s->pos >= (uint32_t)s->frames) { finished(s); if (!s->playing) break; }
                    if (s->channels == 2) { l = s->pcm[s->pos * 2]; r = s->pcm[s->pos * 2 + 1]; }
                    else l = r = s->pcm[s->pos];
                    s_mixbuf[k * 2] += (l * vol) >> 8; s_mixbuf[k * 2 + 1] += (r * vol) >> 8;
                    s->frac += s->step; s->pos += s->frac >> 16; s->frac &= 0xFFFF;
                }
            } else {
                int32_t mono[BLOCK];
                int any, vol = (s->volume * 256) / 100;
                /* events due in this block (block-accurate: ~3 ms at 22 kHz) */
                s->tick_frac += s->ticks_per_frame * (uint32_t)n;
                s->tick += (uint32_t)(s->tick_frac >> 32); s->tick_frac &= 0xFFFFFFFFu;
                while (s->next < s->nev && s->ev[s->next].tick <= s->tick) midi_event(s, &s->ev[s->next++]);
                memset(mono, 0, sizeof(int32_t) * (size_t)n);
                any = render_voices(s, mono, n);
                for (k = 0; k < n; k++) { int32_t v = (mono[k] * vol) >> 8; s_mixbuf[k * 2] += v; s_mixbuf[k * 2 + 1] += v; }
                s->played_frames += (uint32_t)n;
                if (s->next >= s->nev && !any) finished(s);
            }
        }
        for (k = 0; k < n * 2; k++) {
            int32_t v = s_mixbuf[k];
            out[k] = (int16_t)(v > 32767 ? 32767 : (v < -32768 ? -32768 : v));
        }
        out += n * 2; frames -= n;
    }
}
