package com.mygame;

import com.jme.effekseer.EffekseerEmitterControl;
import com.jme.effekseer.driver.EffekseerEmissionDriverGeneric;
import com.jme.effekseer.driver.fun.impl.EffekseerFunctionalEmissionUpdateListener;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.phonon.scene.emitters.PositionalSoundEmitterControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import wf.frk.f3banimation.AnimChannel;
import wf.frk.f3banimation.Animation;
import wf.frk.f3banimation.AnimationGroupControl;
import wf.frk.f3banimation.blending.BlendingFunction;
import wf.frk.f3banimation.blending.TimeFunction;
import wf.frk.f3banimation.utils.TriFunction;
import wf.frk.f3banimation.utils.mutables.MutableFloat;

public class ActionsControl extends AbstractControl{
    private boolean ready=false;
    private BetterCharacterControl character;
    private AnimationGroupControl anims;
    private PositionalSoundEmitterControl footsteps,jumpSound,plasmaSound;
    private boolean withSounds;
    private AssetManager assetManager;
    
    public ActionsControl(AssetManager assetManager,boolean withSounds){
        this.withSounds=withSounds;
        this.assetManager=assetManager;
    }

    public ActionsControl(AssetManager assetManager,boolean withSounds,BetterCharacterControl character){
        this(assetManager,withSounds);
        this.character=character;
    }

    private void initialize() {
        if(ready) return; // initialize only once.
        ready=true;
        if(character == null) character=spatial.getControl(BetterCharacterControl.class);

        anims=AnimationGroupControl.of(spatial);
        anims.setUseHardwareSkinning(true);
        anims.setAction("Stand",TimeFunction.newLooping(() -> 1f),BlendingFunction.newSimple(() -> 1f));
        anims.setAction("Hold",TimeFunction.newClamped(() -> 1f),BlendingFunction.newSimple(() -> 1f));

        if(character != null){
            TriFunction<AnimChannel,Animation,Float,java.lang.Boolean> isRunning=(chan, anim, tpf) -> {
                return character.getVelocity().length() > 1 && character.isOnGround();
            };
            anims.setAction("FPSRun",TimeFunction.newLooping(() -> 1f),BlendingFunction.newToggleFade(0.5f,0.5f,false,() -> 1f,isRunning));
            anims.setAction("Stand",TimeFunction.newLooping(() -> 1f),BlendingFunction.newSimple(() -> 1f));
            anims.setAction("LookUpAndDown",TimeFunction.newSteppingRangeFunction((chan, anim, tpf) -> {
                return (-FastMath.clamp(character.getViewDirection().y ,-1f,1f) )* 0.5f + 0.5f;
            }),BlendingFunction.newSimple(() -> 1f));
            anims.setAction("Jump",TimeFunction.newClamped(() -> 1f),BlendingFunction.newToggleFade(0.2f,0.1f,false,() -> 1f,(chan, anim, tpf) -> {
                return !character.isOnGround();
            }));
            anims.setAction("GunAnim",TimeFunction.newLooping(() -> {
                return character.getVelocity().length() > 1 && character.isOnGround()?1.f:0f;
            }),BlendingFunction.newSimple(() -> 1f));
            
        }


        if(withSounds){
            footsteps=new PositionalSoundEmitterControl(assetManager,"fpstemplate/sounds/walk.f32le");
            spatial.addControl(footsteps);
            footsteps.setReverbEnabled(true);    
            footsteps.setOffset(new Vector3f(0,0.4f,0));
            footsteps.setVolume(0.1f);
            
            jumpSound=new PositionalSoundEmitterControl(assetManager,"fpstemplate/sounds/jump.f32le");
            jumpSound.setOffset(new Vector3f(0,0.4f,0));
            jumpSound.setReverbEnabled(true);
    
            spatial.addControl(jumpSound);
    
             plasmaSound=new PositionalSoundEmitterControl(assetManager,"fpstemplate/sounds/plasma.f32le");
             plasmaSound.setOffset(new Vector3f(0,0.4f,0));
             plasmaSound.setVolume(0.5f);
             plasmaSound.setReverbEnabled(true);
    
            spatial.addControl(plasmaSound);
        }
    }

    public Spatial shot(AssetManager assetManager,Vector3f pos,Vector3f direction,Node parent,PhysicsSpace phy){
        Node bullet=new Node("bullet");
        parent.attachChild(bullet);

        SphereCollisionShape shape=new SphereCollisionShape(0.1f);
        RigidBodyControl rb=new RigidBodyControl(shape,0.1f);
        bullet.addControl(rb);
        phy.add(rb);
        rb.setGravity(Vector3f.ZERO);
        rb.setPhysicsLocation(pos);

        rb.setLinearVelocity(direction.mult(10f));
        if(plasmaSound!=null)plasmaSound.playInstance();


        EffekseerEmitterControl effekt=new EffekseerEmitterControl(assetManager,"fpstemplate/effekts/bullet/bullet.efkefc");
        bullet.addControl(effekt);
        bullet.addControl(new ExplosionControl(assetManager,(x)->x!=spatial.getControl(RigidBodyControl.class)));


        Spatial target=((Node)spatial).getChild("gunTip");
        if(target!=null){
            EffekseerEmitterControl flash=new EffekseerEmitterControl(assetManager,"fpstemplate/effekts/flash/flash.efkefc");
            ((EffekseerEmissionDriverGeneric)flash.getDriver()).setUpdateListener(new EffekseerFunctionalEmissionUpdateListener((tpf, instances) -> {
                if(instances.size() == 0){
                   target.removeControl(flash);
                }
            }));
            target.addControl(flash);
        }

        AnimationGroupControl anims=AnimationGroupControl.of(spatial);
        MutableFloat time=new MutableFloat(0f);
        float fadeInOutSpeed=2f;
        TriFunction<AnimChannel,Animation,Float,Float>  fadeInOut=(chan,anim,tpf)->{
            time.set(time.get()+tpf);
            float x=time.get()*fadeInOutSpeed;
            if(x>1)x=1;
            float y=FastMath.sin(x*FastMath.PI);
            return y;
        } ;
        anims.setAction("Recoil", TimeFunction.newClamped(()->1f), fadeInOut);
        
        return bullet;


    }

    private float t=0;
    private boolean wasOnGround=true;

    @Override
    protected void controlUpdate(float tpf) {
        initialize();
        if(!ready) return;
        
        if(character != null){
            if(jumpSound != null){
                if(!character.isOnGround() && wasOnGround) jumpSound.playInstance();
                wasOnGround=character.isOnGround();
            }
            if(footsteps != null){
                boolean walking=(character.isOnGround() && character.getVelocity().length() > 0.1);
                if(walking){
                    t+=tpf;
                    if(t > 0.3){
                        t=0;
                        footsteps.playInstance();
                    }
                }
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}