package com.mygame;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class FirstPersonMovementsControl extends AbstractControl implements ActionListener,AnalogListener{

    private boolean left = false, right = false, up = false, down = false, run = false;
    private final Vector3f walkDirection = new Vector3f();
    private final float walkSpeed = 10.0f;
    private final float runSpeed = 20.0f;
    private final float rotationSpeed=0.5f;

    private  final InputManager inputManager;
    private boolean ready=false;
    private BetterCharacterControl characterControl;
    private float isInAir=0;
    private Runnable attackCallback;

    public FirstPersonMovementsControl(InputManager inputManager,Runnable attackCallback){
        this.inputManager=inputManager;
        this.attackCallback=attackCallback;
    }
    
    private void initialize(){
        if(ready)return;
        ready=true;

        characterControl=spatial.getControl(BetterCharacterControl.class);
        if(characterControl==null){
            System.err.println(getClass()+" can be attached only to a spatial that has a BetterCharacterControl");
            return;
        }


        inputManager.setCursorVisible(false);

        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));

        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Run", new KeyTrigger(KeyInput.KEY_LSHIFT));

        inputManager.addMapping("Rotate_Left", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("Rotate_Right", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("Rotate_Up", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("Rotate_Down", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        inputManager.addListener(this, "Left");
        inputManager.addListener(this, "Right");
        inputManager.addListener(this, "Up");
        inputManager.addListener(this, "Down");

        inputManager.addListener(this, "Jump");
        inputManager.addListener(this, "Run");
        inputManager.addListener(this, "Attack");

        inputManager.addListener(this, "Rotate_Left");
        inputManager.addListener(this, "Rotate_Right");
        inputManager.addListener(this, "Rotate_Up");
        inputManager.addListener(this, "Rotate_Down");
        
    }

    public void destroy(){
        inputManager.deleteMapping("Left");
        inputManager.deleteMapping("Right");
        inputManager.deleteMapping("Up");
        inputManager.deleteMapping("Down");

        inputManager.deleteMapping("Jump");
        inputManager.deleteMapping("Run");
        inputManager.deleteMapping("Attack");

        inputManager.deleteMapping("Rotate_Left");
        inputManager.deleteMapping("Rotate_Right");
        inputManager.deleteMapping("Rotate_Up");
        inputManager.deleteMapping("Rotate_Down");

        inputManager.removeListener(this);
    }


    final float angles[]={0,0,0};
    final Quaternion tmpRot=new Quaternion();
    @Override
    public void onAnalog(String name, float value, float tpf) {
        value=value*rotationSpeed;
        switch(name){

            case "Rotate_Left":{
                angles[1]+=value;
                break;
            }

            case "Rotate_Right":{
                angles[1]-=value;
                break;
            }

            case "Rotate_Up":{
                angles[0]-=value;
                break;
            }

            case "Rotate_Down":{
                angles[0]+=value;
                break;
            }

        }
        if(angles[0]>1.1)angles[0]=1.1f;
        if(angles[0]<-0.85)angles[0]=-0.85f;
        
        Vector3f v=characterControl.getViewDirection();
        v.set(Vector3f.UNIT_Z);
        tmpRot.fromAngles(angles);
        tmpRot.multLocal(v);
        characterControl.setViewDirection(v);
    }

    @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
        switch (binding) {
            case "Left": {
                left = isPressed;
                break;
            }
            case "Right": {
                right = isPressed;
                break;
            }
            case "Up": {
                up = isPressed;
                break;
            }
            case "Down": {
                down = isPressed;
                break;
            }
            case "Attack":{
                if(isPressed){
                    attackCallback.run();
                }
                break;

            }
            case "Jump": {
                if (isPressed) {
                    characterControl.jump();
                    characterControl.setPhysicsDamping(0);
                }
                break;
            }
            case "Run": {
                run = isPressed;
                break;
            }
        }
    }


    private final Quaternion tmpQtr=new Quaternion();
    private final Vector3f tmpV3=new Vector3f();
    
    @Override
    protected void controlUpdate(float tpf) {
        initialize();
        
        // Using a float here helps us achieve smoother transitions on rough terrains.
        if(!characterControl.isOnGround())  isInAir+=tpf;
        else isInAir=0;        

        if(isInAir>=.1f){ // On air: don't walk :)
            isInAir=.1f;
            characterControl.setPhysicsDamping(0f);
            characterControl.setWalkDirection(Vector3f.ZERO);
        }else{ // On ground walk
            float speed = run ? runSpeed : walkSpeed;

            walkDirection.set(0, 0, 0);
            if (left) walkDirection.addLocal(1,0,0);
            if (right) walkDirection.addLocal(-1,0,0);
            if (up) walkDirection.addLocal(0,0,1);
            if (down)   walkDirection.addLocal(0,0,-1);
            
            tmpV3.set(characterControl.getViewDirection());
            tmpV3.y=0; // Remove y component
            tmpV3.normalizeLocal();
    
            tmpQtr.loadIdentity();
            tmpQtr.lookAt(tmpV3, Vector3f.UNIT_Y);
            tmpQtr.multLocal(walkDirection);
            walkDirection.multLocal(speed);

            characterControl.setWalkDirection(walkDirection);
            characterControl.setPhysicsDamping(0.9f);

        }
        
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }
    
}