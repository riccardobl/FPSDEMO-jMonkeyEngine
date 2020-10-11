package com.mygame;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.function.Consumer;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.light.LightProbe;
import com.jme3.material.MatParam;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class SimplerEnvBaker extends BaseAppState{
    private ArrayList<Runnable> queue=new ArrayList<Runnable>();
    private boolean baking=false;
    private File cacheFolder;
    private String cacheAssetFolder;

    public void setAssetCacheFolder(String path) {
        this.cacheAssetFolder=path;
        if(!this.cacheAssetFolder.endsWith("/"))this.cacheAssetFolder+="/";
    }

    public void setCacheFolder(String path) {
        File f=new File(path);
        if(f.exists() && f.isDirectory()){
            cacheFolder=f;
            System.err.println("Use location " + path + " as envmap cache folder");

        }else{
            System.err.println("Can't use location " + path + " as envmap cache folder");
        }
    }

    protected String hash(Spatial sp,int resolution) {
        StringBuilder hash=new StringBuilder();;
        sp.depthFirstTraversal(sx -> {
            hash.append("_").append(sx.getName());
            hash.append("_").append(sx.getClass().getName());
            hash.append("_").append(sx.getWorldTransform().hashCode());
            if(sx instanceof Geometry){
                Geometry geo=(Geometry)sx;
                for(MatParam p:geo.getMaterial().getParams()){
                    hash.append("_mparam").append(p.getName());
                    hash.append(":").append(p.getValueAsString());
                }
            }
        });
        try{
            MessageDigest md=MessageDigest.getInstance("MD5");
            md.update(hash.toString().getBytes());
            byte[] digest = md.digest();
            BigInteger bigInt = new BigInteger(1,digest);
            String hashs = bigInt.toString(16);
            while(hashs.length() < 32 )hashs = "0"+hashs;	
            return hashs+"x"+resolution;
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return null;
    }

    protected void setCache(Spatial sp,LightProbe probe,int resolution){
        if(cacheFolder==null)return;
        try{
            String hash=hash(sp,resolution);
            System.out.println("Saving "+hash+" to cache");
            File dest=new File(cacheFolder,hash+".jmeEnvCache");
            BinaryExporter exp=BinaryExporter.getInstance();
            exp.save(probe,dest);
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    protected LightProbe getCache(AssetManager assetManager,Spatial sp,int resolution){
        String hash=hash(sp,resolution);
        String path=hash+".jmeEnvCache";
        
        InputStream is=null;
        if(cacheAssetFolder!=null){
            AssetInfo ainfo=null;
            try{
                System.out.println("Loading "+path+" from assets");
                ainfo=assetManager.locateAsset(new AssetKey(cacheAssetFolder+path));
            }catch(Exception e){}
            if(ainfo!=null){
                is=ainfo.openStream();
            }else{
                System.out.println("Can't find " +cacheAssetFolder+path);
            }
        }

        if(is==null){
            if(cacheFolder==null)return null;
            System.out.println("Loading "+path+" from cache");
            File src=new File(cacheFolder,path);
            if(src.exists()){
                try{
                    is=new BufferedInputStream(new FileInputStream(src));
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
       
        if(is!=null){
            try{
                BinaryImporter imp=BinaryImporter.getInstance();
                LightProbe probe=(LightProbe)imp.load(is);
                return probe;
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        return null;

    }

    public  void bakeEnv(AppStateManager stateManager,AssetManager assetManager, Spatial environment, int res, Vector3f pos, Consumer<LightProbe> callback) {
        LightProbe lp=getCache(assetManager,environment,res);
        if(lp!=null){
            callback.accept(lp);
            return ;
        }
        queue.add(() -> {
            baking=true;
            Node rootBaker=new Node("BakerRoot");
            rootBaker.attachChild(environment.clone());

            stateManager.attach(new EnvironmentCamera(res,pos){
                @Override
                protected void initialize(Application app) {
                    super.initialize(app);
                    EnvironmentCamera envcam=this;
                    LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class),rootBaker,EnvMapUtils.GenerationType.Fast,new JobProgressAdapter<LightProbe>(){

                        @Override
                        public void progress(double value) {
                            System.out.println("Baking " + value);
                        }

                        @Override
                        public void done(LightProbe lightProbe) {
                            System.out.println("Baked");
                            lightProbe.getArea().setRadius(1000f);
                            setCache(environment,lightProbe,res);
                            callback.accept(lightProbe);
                            stateManager.detach(envcam);
                            baking=false;
                        }
                    });
                }

                @Override
                public void render(final RenderManager renderManager) {
                    rootBaker.updateGeometricState();
                    super.render(renderManager);
                }
            });
        });
    }

    @Override
    public void update( float tpf ) {
        if(!baking&&queue.size()>0)queue.remove(0).run();
    }

    @Override
    protected void initialize(Application app) {

    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }
}