package com.anyplayer.ellaplayer.base.player;

import android.media.MediaPlayer;

/**********************************
 * Copyright 2018 SH-HG Inc.
 * Author:HangCao(Melvin)         
 * Email:hang.yasuo@gmail.com     
 * ProNm:EllaPlayer          
 * Date: 2018/8/13.
 **********************************/
//TODO 这个要替换成IJKPlayer
public class EllaIJKPlayer extends MediaPlayer implements EllaPlayer {
    @Override
    public String name() {
        return "IJKPlayer";
    }
}
