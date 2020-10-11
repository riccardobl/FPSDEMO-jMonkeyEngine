package com.mygame;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

public class CameraFollowSpatial extends AbstractControl{
    private boolean ready=false;
    private Camera cam;
    private Vector3f offset,direction;
    private BetterCharacterControl character;
    private Spatial cameraNode;

    public CameraFollowSpatial(Camera cam){
        this.cam=cam;
    }

    public void setOffset(Vector3f offset){
        this.offset=offset;
    }

    public void setDirection(Vector3f direction){
        this.direction=direction;
    }

    private void initialize() {
        if(ready) return; // initialize only once.
        ready=true;

        if(offset == null){
            // Offset is null, search if there is a 'camera' node in this spatial
            if(spatial instanceof Node){
                Node n=(Node)spatial;
                Spatial cameraNode=n.getChild("camera");
                if(cameraNode != null){
                    this.cameraNode=cameraNode;
                }
            }
        }

        // Find character control in the spatial
        if(character == null){
            spatial.depthFirstTraversal(sx -> {
                BetterCharacterControl c=sx.getControl(BetterCharacterControl.class);
                if(c != null) character=c;
            });
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        initialize(); // initialize if needed.
        Vector3f loc=cam.getLocation();
        if(offset!=null){
            // Location is spatial location + offset
            loc.set(spatial.getWorldTranslation());
            loc.addLocal(offset);
        }else{
             // Location is the camera node location
            loc.set(cameraNode.getWorldTranslation());
        }
        cam.setLocation(loc);

        // if character exists: Direction is character direction
        if(character != null){
            cam.lookAtDirection(character.getViewDirection(),Vector3f.UNIT_Y);
        }else if(cameraNode!=null){ // If cameranode exists: Direction is cameranode direction
            cam.setRotation(cameraNode.getWorldRotation());
        }else{ // direction is the specified direction
            cam.lookAtDirection(direction,Vector3f.UNIT_Y);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }



}