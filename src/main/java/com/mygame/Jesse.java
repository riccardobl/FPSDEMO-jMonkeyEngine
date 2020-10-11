package com.mygame;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.InputManager;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import wf.frk.f3banimation.AnimationGroupControl;
import wf.frk.f3banimation.SkeletonViewer;
import wf.frk.f3banimation.blending.BlendingFunction;
import wf.frk.f3banimation.blending.TimeFunction;

public class Jesse extends Node{

    public Jesse(AssetManager assetManager){
        Spatial jj=assetManager.loadModel("fpstemplate/models/Jesse.f3b");
        jj.setLocalScale(0.4f);
        attachChild(jj);
        setCullHint(CullHint.Never);
    }
    
    public void loadNPCLogic(
        AssetManager assetManager,
        Node rootNode,
        PhysicsSpace pspace
    ){
        BoundingBox jesseBbox=(BoundingBox)getWorldBound();
        
        
        CollisionShape shape=new BoxCollisionShape(jesseBbox.getExtent(null));
        
        // Since the center of jesse is on the feet and the center of the BoxCollisionShape is in the middle, we need to offset the shape
        CompoundCollisionShape offsettedShape=new CompoundCollisionShape();
        offsettedShape.addChildShape(shape, jesseBbox.getCenter());

        RigidBodyControl rb = new RigidBodyControl(offsettedShape, 50f);
        rb.setAngularDamping(1f); // No rotation
        addControl(rb);
        addControl(new SkeletonViewer(assetManager,true,rootNode));

        AnimationGroupControl anims=AnimationGroupControl.of(this);
        anims.setUseHardwareSkinning(true);
        anims.setAction("Stand",TimeFunction.newLooping(() -> 1f),BlendingFunction.newSimple(() -> 1f));
    }

    public void loadFPSLogicWorld(
        AssetManager assetManager,
        InputManager inputManager,
        Camera cam,
        Camera fpsCam,
        Spatial fpsJesse,
        Node rootNode,
        PhysicsSpace pspace
    ){
        BoundingBox jesseBbox=(BoundingBox)getWorldBound();
        BetterCharacterControl characterControl = new BetterCharacterControl(jesseBbox.getXExtent(), jesseBbox.getYExtent()*2, 50f);
        characterControl.setJumpForce(new Vector3f(0, 600, 0));
        addControl(characterControl);

        // Load character logic
        addControl(new FirstPersonMovementsControl(inputManager,()->{
            fpsJesse.getControl(ActionsControl.class).shot( assetManager,cam.getLocation().add(cam.getDirection().mult(1)),cam.getDirection(),rootNode,pspace);
        }));
        addControl(new CameraFollowSpatial(cam));
        addControl(new ActionsControl(assetManager,false));
        addControl(new FPSViewControl(FPSViewControl.Mode.WORLD_SCENE));
    }
  
    public void loadFPSLogicFPSView(
        AssetManager assetManager,
        InputManager inputManager,
        Camera cam,
        Camera fpsCam,
        Spatial jesse,
        Node rootNode,
        PhysicsSpace pspace
    ){
        addControl(new AbstractControl(){ 
            protected void controlUpdate(float tpf) {
                setLocalTransform(jesse.getWorldTransform());
                fpsCam.setLocation(cam.getLocation());
                fpsCam.lookAtDirection(cam.getDirection(),cam.getUp());
            }
            protected void controlRender(RenderManager rm, ViewPort vp) { }            
        });
        addControl(new FPSViewControl(FPSViewControl.Mode.FPS_SCENE));        
        addControl(new ActionsControl(assetManager,true,jesse.getControl(BetterCharacterControl.class)));

    }
}