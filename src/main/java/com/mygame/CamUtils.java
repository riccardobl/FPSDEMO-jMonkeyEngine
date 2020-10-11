package com.mygame;

import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;

public class CamUtils{
    public static void setFov(Camera cam,float fov,float frustumNear,float frustumFar){
        float aspect = (float)cam.getWidth() / (float)cam.getHeight();
        float radfov=fov * FastMath.PI / 180.0F;
        float fovy=(float)(2.0F * FastMath.atan(FastMath.tan(radfov / 2.0F) * (float)cam.getHeight() / (float)cam.getWidth()));
        fovy=FastMath.ceil(fovy * 180.0F / FastMath.PI);
        cam.setFrustumPerspective(fovy, aspect, frustumNear, frustumFar);
        assert frustumNear<frustumFar;

    }
}