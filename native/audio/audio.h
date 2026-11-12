/* amiga-j2me audio: a mixer for PCM sounds (WAV) and Standard MIDI Files
 * played by a small built-in synthesiser (integer only: pulse, saw and
 * triangle voices with envelopes, noise drums). Plain C, no OS calls. GPLv2. */
#ifndef AJ_AUDIO_H
#define AJ_AUDIO_H
#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void aud_init(int rate);                 /* output rate in Hz */

/* Creates a sound from a WAV file or a Standard MIDI File (the data is
 * copied / decoded). Returns a handle > 0, or 0 when it is neither. */
int  aud_create(const uint8_t *data, int len);
void aud_destroy(int h);

/* loops: number of times to play, -1 = forever. volume 0..100. */
void aud_play(int h, int loops, int volume);
void aud_stop(int h);                    /* stays at its position */
void aud_rewind(int h);
void aud_set_volume(int h, int volume);
int  aud_is_playing(int h);
int  aud_duration_ms(int h);             /* -1 = unknown */
int  aud_position_ms(int h);

/* Mixes `frames` stereo 16-bit frames (interleaved) into out. */
void aud_mix(int16_t *out, int frames);

#ifdef __cplusplus
}
#endif
#endif
