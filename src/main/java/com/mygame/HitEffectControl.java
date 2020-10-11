package com.mygame;

import com.jme.effekseer.EffekseerEmitterControl;
import com.jme.effekseer.driver.EffekseerEmissionDriverGeneric;
import com.jme.effekseer.driver.fun.impl.EffekseerGenericSpawner;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.phonon.PhononSettings.PhononDirectOcclusionMode;
import com.jme3.phonon.scene.emitters.PositionalSoundEmitterControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
A control that plays an effect on impact
*/
public class HitEffectControl extends AbstractControl implements PhysicsCollisionListener{
    private AssetManager assetManager;
    private boolean  ready=false;
    private float minStrength=0.2f;// minimum force to trigger the sound
    private PositionalSoundEmitterControl hitSound;
    private String sound,effect;
    private EffekseerGenericSpawner smokeSpawner;

    public HitEffectControl(AssetManager assetManager){
        this(assetManager,"fpstemplate/sounds/hit.f32le","fpstemplate/effekts/smoke/smoke.efkefc");

    }
    public HitEffectControl(AssetManager assetManager,String sound,String effect){
        this.assetManager=assetManager;
        this.sound=sound;
        this.effect=effect;
        
    }


    // Physics engine callback
    @Override
    public void collision(PhysicsCollisionEvent event) {
        RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);
        // Check if the collision interests this rigidbody 
        if(!((event.getObjectA() == rb) || (event.getObjectB() == rb))) return; // if not: return
        if(event.getAppliedImpulse()>minStrength)  bonk();
    }

    private void initialize(){
        if(ready)return; // initialize only once.
        ready=true;
        RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);
        if(rb==null){
            System.err.println(getClass()+" can be attached only to a spatial that has a rigidbody.");
            return;
        }
        rb.getPhysicsSpace().addCollisionListener(this);

        hitSound=new PositionalSoundEmitterControl(assetManager,sound);
        hitSound.setDirectOcclusionMode(PhononDirectOcclusionMode.IPL_DIRECTOCCLUSION_NONE);
        hitSound.setReverbEnabled(true);
        hitSound.setVolume(1.f);
        spatial.addControl(hitSound);

        // Load smoke Effect
        EffekseerEmitterControl smoke=new EffekseerEmitterControl(assetManager,effect);
        EffekseerEmissionDriverGeneric driver=(EffekseerEmissionDriverGeneric)smoke.getDriver();
        // When the effect is over, destroy everything
        smokeSpawner=new EffekseerGenericSpawner();
        smokeSpawner.autoSpawner(false);
        smokeSpawner.maxInstances(1);
        driver.spawner(smokeSpawner);
        spatial.addControl(smoke);
    }

    // hit sound!
    private void bonk(){
        hitSound.play();
        smokeSpawner.spawnNow();
    }



    // Loop to run the things
    @Override
    protected void controlUpdate(float tpf) {
        initialize(); // initialize if needed.
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}