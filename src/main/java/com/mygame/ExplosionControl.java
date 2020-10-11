package com.mygame;

import java.util.function.Function;

import com.jme.effekseer.EffekseerEmitterControl;
import com.jme.effekseer.driver.EffekseerEmissionDriverGeneric;
import com.jme.effekseer.driver.fun.impl.EffekseerFunctionalEmissionUpdateListener;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.phonon.PhononSettings.PhononDirectOcclusionMethod;
import com.jme3.phonon.scene.emitters.PositionalSoundEmitterControl;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * A control that makes things go boom
 */
public class ExplosionControl extends AbstractControl implements PhysicsCollisionListener{
    private AssetManager assetManager;
    private float phyExplosionDelay=-1;
    private Function<PhysicsCollisionObject,Boolean> filter;
    private boolean  ready=false;
    private boolean exploded=false;

    public ExplosionControl(AssetManager assetManager,Function<PhysicsCollisionObject,Boolean> filter){
        this.assetManager=assetManager;
        this.filter=filter;
    }


    // Physics engine callback
    @Override
    public void collision(PhysicsCollisionEvent event) {
        RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);
        // Check if the collision interests this rigidbody against a non filtered rigidbody
        if(!((event.getObjectA() == rb && filter.apply(event.getObjectB())) || (event.getObjectB() == rb && filter.apply(event.getObjectA())))) return; // if not: return
        ignite();
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
    }

    // Graphical explosion
    private void ignite(){
        if(exploded) return; // explode only once
        exploded=true;
                
        // Load explosion sound
        PositionalSoundEmitterControl expl=new PositionalSoundEmitterControl(assetManager,"fpstemplate/sounds/Explosion.f32le");
        expl.setSourceRadius(10f);
        expl.setDirectOcclusionMethod(PhononDirectOcclusionMethod.IPL_DIRECTOCCLUSION_VOLUMETRIC);
        expl.setReverbEnabled(true);
        spatial.addControl(expl);
        expl.setVolume(4.f);
        expl.setCustomDirectSoundPathFunction((path) -> {
            path.distanceAttenuation*=100;
            if(path.distanceAttenuation > 1) path.distanceAttenuation=1;
        });
        expl.play();

        // Load explosion Effect
        EffekseerEmitterControl flame=new EffekseerEmitterControl(assetManager,"fpstemplate/effekts/pierre01/flame.efkefc");
        EffekseerEmissionDriverGeneric driver=(EffekseerEmissionDriverGeneric)flame.getDriver();
        // When the effect is over, destroy everything
        driver.setUpdateListener(new EffekseerFunctionalEmissionUpdateListener((tpf, instances) -> {
            if(instances.size() == 0){
                destroy();
                System.out.println("Explosion over");
            }
        }));
        spatial.addControl(flame);

        // Stop the rigidbody in position
        RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);
        rb.setEnabled(false);

        // Delay physical explosion (we do this to sync it with the animation and sound that have an ignition phase)
        phyExplosionDelay=2f;
    }

    //  Destroy everything
    private void destroy(){
        RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);
        rb.getPhysicsSpace().removeCollisionListener(this);
        rb.getPhysicsSpace().removeAll(spatial);
        spatial.removeFromParent();
    }


    // Physical explosion
    private void boom(){
        RigidBodyControl rb=spatial.getControl(RigidBodyControl.class);
        for(PhysicsRigidBody b:rb.getPhysicsSpace().getRigidBodyList()){ // for each rigidbody in the world
            if(b.getMass() == 0) continue; // if not static
            Vector3f expCenter2Body=b.getPhysicsLocation().subtract(rb.getPhysicsLocation()); // get explosion vector
            float distance=expCenter2Body.length(); // get distance
            float explosionRadius=10f; // explosion radius
            float baseStrength=100f; // explosion strength
            if(distance < explosionRadius){ // if the object is within the explosion radius
                // apply proportional explosion force
                float strength=(1.f - FastMath.clamp(distance / explosionRadius,0,1)) * baseStrength;
                b.setLinearVelocity(expCenter2Body.normalize().mult(strength));
            }
        }
    }


    // Loop to run the things
    @Override
    protected void controlUpdate(float tpf) {
        initialize(); // initialize if needed.
        if(exploded){
            //Delayed ignition
            if(phyExplosionDelay> 0){
                phyExplosionDelay-=tpf;
                if(phyExplosionDelay <= 0)  boom();
            }
        }        
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}