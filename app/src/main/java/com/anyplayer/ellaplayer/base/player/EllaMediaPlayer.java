package com.anyplayer.ellaplayer.base.player;

import android.media.MediaPlayer;

/**********************************
 * Copyright 2018 SH-HG Inc.
 * Author:HangCao(Melvin)         
 * Email:hang.yasuo@gmail.com     
 * ProNm:EllaPlayer          
 * Date: 2018/8/13.                 
 **********************************/
public class EllaMediaPlayer extends MediaPlayer implements EllaPlayer {

    @Override
    public String name() {
        return "MediaPlayer";
    }
}
