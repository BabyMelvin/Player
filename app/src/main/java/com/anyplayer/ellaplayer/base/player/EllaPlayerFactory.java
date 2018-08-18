package com.anyplayer.ellaplayer.base.player;

import android.content.res.Resources;

/**********************************
 * Copyright 2018 SH-HG Inc.
 * Author:HangCao(Melvin)         
 * Email:hang.yasuo@gmail.com     
 * ProNm:EllaPlayer          
 * Date: 2018/8/13.                 
 **********************************/
public class EllaPlayerFactory {
    public final static String MEDIA_PLAYER = "MediaPlayer";
    public final static String IJK_PLAYER = "IJKPlayer";
    public final static String EXO_PLAYER = "EXOPlayer";

    public static EllaPlayer build(String playName) {
        switch (playName) {
            case MEDIA_PLAYER:
                return new EllaMediaPlayer();
            case IJK_PLAYER:
                //TODO
                break;
            case EXO_PLAYER:
                //TODO
                break;
            default:
                throw new Resources.NotFoundException("不支持改播放器：" + playName);
        }
        return null;
    }
}
