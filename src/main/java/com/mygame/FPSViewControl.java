package com.mygame;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.AbstractControl;

public class FPSViewControl extends AbstractControl{
    public static enum Mode{
        FPS_SCENE,
        WORLD_SCENE
    }
    private boolean ready=false;
    private Mode mode=Mode.WORLD_SCENE;
    public FPSViewControl(Mode mode){
        this.mode=mode;
    }

    private void initialize() {
        if(ready) return; // initialize only once.
        ready=true;
        if(mode==Mode.WORLD_SCENE)spatial.setCullHint(CullHint.Always);
        else ((Node)spatial).getChild("body").setCullHint(CullHint.Always);       
    }

    @Override
    protected void controlUpdate(float tpf) {
        initialize();

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }
}