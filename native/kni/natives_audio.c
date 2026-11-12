/* KNI natives of aj.Native: sounds (native/audio over the HAL output). GPLv2. */
#include "kni.h"
#include "sni.h"
#include "hal.h"
#include "audio.h"

static int s_state;      /* 0 = not tried, 1 = open, -1 = no audio */

static void fill(int16_t *stereo, int frames, void *user)
{
    (void)user;
    aud_mix(stereo, frames);
}

static int audio_ready(void)
{
    if (s_state == 0) {
        int rate = hal_audio_open();
        if (rate > 0) { aud_init(rate); hal_audio_set_fill(fill, 0); s_state = 1; }
        else s_state = -1;
    }
    return s_state == 1;
}

KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_sndCreate(void)
{
    int h = 0;
    if (audio_ready()) {
        KNI_StartHandles(1);
        KNI_DeclareHandle(hdata);
        KNI_GetParameterAsObject(1, hdata);
        h = aud_create((const uint8_t *)SNI_GetRawArrayPointer((jarray)hdata) + KNI_GetParameterAsInt(2),
                       KNI_GetParameterAsInt(3));
        KNI_EndHandles();
    }
    KNI_ReturnInt(h);
}

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_sndClose(void)  { aud_destroy(KNI_GetParameterAsInt(1)); KNI_ReturnVoid(); }
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_sndPlay(void)
{
    aud_play(KNI_GetParameterAsInt(1), KNI_GetParameterAsInt(2), KNI_GetParameterAsInt(3));
    KNI_ReturnVoid();
}
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_sndStop(void)   { aud_stop(KNI_GetParameterAsInt(1)); KNI_ReturnVoid(); }
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_sndRewind(void) { aud_rewind(KNI_GetParameterAsInt(1)); KNI_ReturnVoid(); }
KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_sndVolume(void)
{
    aud_set_volume(KNI_GetParameterAsInt(1), KNI_GetParameterAsInt(2));
    KNI_ReturnVoid();
}
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_sndPlaying(void)  { KNI_ReturnInt(aud_is_playing(KNI_GetParameterAsInt(1))); }
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_sndDuration(void) { KNI_ReturnInt(aud_duration_ms(KNI_GetParameterAsInt(1))); }
KNIEXPORT KNI_RETURNTYPE_INT Java_aj_Native_sndPosition(void) { KNI_ReturnInt(aud_position_ms(KNI_GetParameterAsInt(1))); }

KNIEXPORT KNI_RETURNTYPE_VOID Java_aj_Native_audioUpdate(void)
{
    if (s_state == 1) hal_audio_update();
    KNI_ReturnVoid();
}
