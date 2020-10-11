package com.mygame;

import java.util.ArrayList;

import com.jayfella.filter.MipMapBloom.MipmapBloomFilter;
import com.jayfella.filter.MipMapBloom.MipmapBloomFilter.GlowMode;
import com.jme.effekseer.EffekseerRenderer;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.phonon.Phonon;
import com.jme3.phonon.PhononRenderer;
import com.jme3.phonon.desktop_javasound.JavaSoundPhononSettings;
import com.jme3.phonon.scene.emitters.SoundEmitterControl;
import com.jme3.phonon.scene.material.PhononMaterialPresets;
import com.jme3.phonon.scene.material.SingleMaterialGenerator;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

import wf.frk.f3b.jme3.F3bKey;
import wf.frk.f3b.jme3.F3bLoader;
import wf.frk.f3b.jme3.physicsloader.impl.bullet.BulletPhysicsLoader;
import wf.frk.f3b.jme3.runtime.F3bRuntimeLoader;

public class Main extends SimpleApplication{

    public static void main(String[] args) {

        Main app=new Main();

        AppSettings settings=new AppSettings(true);
        settings.setTitle("jMonkeyEngine - FPS Demo");
        settings.setResolution(1280,720);
        settings.setGammaCorrection(true);
        settings.setSamples(2);
        // settings.setFrameRate(120);

        app.setShowSettings(true);
        app.setSettings(settings);
        app.start();
    }

    public Main(){
        super(new StatsAppState(),new AudioListenerState(),new DebugKeysAppState());
    }

    public void initFilters(FilterPostProcessor fpp) {
        CartoonEdgeFilter cartoonEdge=new CartoonEdgeFilter();
        cartoonEdge.setEdgeColor(ColorRGBA.Black);
        fpp.addFilter(cartoonEdge);

        // SSAOFilter ssaoFilter = new SSAOFilter(2.9299974f,25f,5.8100376f,0.091000035f);
        // fpp.addFilter(ssaoFilter);

        ToneMapFilter toneMapFilter=new ToneMapFilter(new Vector3f(11.2f,11.2f,10.f).mult(0.7f));
        fpp.addFilter(toneMapFilter);

        MipmapBloomFilter bloomFilter=new MipmapBloomFilter();
        bloomFilter.setGlowMode(GlowMode.SceneAndObjects);
        bloomFilter.setBloomIntensity(0.8f,0.5f);
        fpp.addFilter(bloomFilter);

        DirectionalLightShadowFilter dlsf=new DirectionalLightShadowFilter(assetManager,1024,2);
        fpp.addFilter(dlsf);
        dlsf.setRenderBackFacesShadows(false);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF8);
        dlsf.setEnabledStabilization(false);
        dlsf.setShadowIntensity(0.6f);
        dlsf.setShadowCompareMode(CompareMode.Hardware);

        // Screenspace AA pass. Helpful to remove some aliasing created by the filters and what cannot be catched by MSAA
        for(int i=0;i < 1;i++){
            FXAAFilter fxaaFilter=new FXAAFilter();
            fpp.addFilter(fxaaFilter);
        }

    }

    @Override
    public void simpleInitApp() {
        // Rendered configurations
        getRenderManager().setPreferredLightMode(TechniqueDef.LightMode.SinglePassAndImageBased);
        // Unless you have a lot of lights, set this to the max numbers of lights you will have at the same time in your scene
        // If the lights exceet the batch size, the engine will automatically execute another render pass with the remaining lights
        getRenderManager().setSinglePassLightBatchSize(2);

        // Init f3b model loader
        F3bLoader.init(assetManager);

        // Init SteamAudio engine
        PhononRenderer soundEngine=null;
        JavaSoundPhononSettings soundSettings=null;
        try{
            soundSettings=new JavaSoundPhononSettings();
            soundSettings.maxConvolutionSources=0;
            soundSettings.numDiffuseSamples=16;
            soundSettings.numRays=512;
            soundSettings.numOcclusionSamples=16;
            soundSettings.frameSize=2048;
            soundSettings.materialGenerator=new SingleMaterialGenerator(PhononMaterialPresets.metal);
            soundEngine=Phonon.init(soundSettings,this);
        }catch(Exception e){
            throw new RuntimeException("Can't load audio engine",e);
        }
        assert soundEngine != null && soundSettings != null;

        // Init physics engine
        BulletAppState bulletAppState=new BulletAppState();
        bulletAppState.setDebugEnabled(false); // enable to visualize physics meshes
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0,-18f,0)); // Gravity is high to make it feel snappier

        // The viewports
        // In an fps game we usually need two viewports
        // The first one is the main scene viewport and the second one the first person viewport
        // Scene Viewport:
        //      this.viewPort 
        CamUtils.setFov(cam,101.0f,0.01f,1000f);

        // FPS Viewport
        Node fpsRoot=new Node("FPSRoot");
        Camera fpsCam=cam.clone();
        CamUtils.setFov(fpsCam,80.0f,0.001f,100f); // we can use a different fov for the fpscam
        ViewPort fpsView=renderManager.createMainView("FPSView",fpsCam);
        fpsView.setClearDepth(true); // When this viewport is rendered it needs to clear the depth, this will prevent the gun from clipping through the world (there won't be any world depth to test against..)
        fpsView.getScenes().add(fpsRoot);

        // The post processing
        FilterPostProcessor mainFpp=new FilterPostProcessor(assetManager);
        mainFpp.setNumSamples(settings.getSamples());
        viewPort.addProcessor(mainFpp);

        // Particles
        // Effekseer needs to be initialized here, since it has to be the first filter in the FPP
        EffekseerRenderer effekseerRenderer=EffekseerRenderer.addToViewPort(stateManager,viewPort,assetManager,settings.isGammaCorrection());
        effekseerRenderer.setSoftParticles(0.9f,2f);
        effekseerRenderer.setAsync(4);

        EffekseerRenderer effekseerRendererFPS=EffekseerRenderer.addToViewPort(stateManager,fpsView,assetManager,settings.isGammaCorrection());
        effekseerRendererFPS.setHardParticles();
        effekseerRenderer.setAsync(4);

        // Add some filters
        initFilters(mainFpp);

        // Load the map
        resetMap(rootNode,fpsRoot);
        loadMap(soundSettings,rootNode,fpsRoot,mainFpp);

        // Load the character
        Jesse jesse=new Jesse(assetManager);
        Jesse fpsJesse=(Jesse)jesse.clone();
        jesse.loadFPSLogicWorld(assetManager,inputManager,cam,fpsCam,fpsJesse,rootNode,bulletAppState.getPhysicsSpace());
        fpsJesse.loadFPSLogicFPSView(assetManager,inputManager,cam,fpsCam,jesse,rootNode,bulletAppState.getPhysicsSpace());

        // Add character to world
        bulletAppState.getPhysicsSpace().addAll(jesse);
        rootNode.attachChild(jesse);
        fpsRoot.attachChild(fpsJesse);

        rootNode.addControl(new AbstractControl(){
            protected void controlUpdate(float tpf) {
                fpsRoot.updateLogicalState(tpf);
                fpsRoot.updateGeometricState();
            }

            protected void controlRender(RenderManager rm, ViewPort vp) {
            }
        });


        Jesse npcJesse=new Jesse(assetManager);
        npcJesse.loadNPCLogic(assetManager,  rootNode, bulletAppState.getPhysicsSpace());
        npcJesse.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(10,0,0));
        bulletAppState.getPhysicsSpace().addAll(npcJesse);
        rootNode.attachChild(npcJesse);
    }

    private void resetMap(Node rootNode, Node fpsRootNode) {
        // Clear the roots
        rootNode.detachAllChildren();
        fpsRootNode.detachAllChildren();

        for(Light l:rootNode.getLocalLightList())
            rootNode.removeLight(l);
        for(int i=0;i < rootNode.getNumControls();i++)
            rootNode.removeControl(rootNode.getControl(i));

        for(Light l:fpsRootNode.getLocalLightList())
            fpsRootNode.removeLight(l);
        for(int i=0;i < fpsRootNode.getNumControls();i++)
            fpsRootNode.removeControl(fpsRootNode.getControl(i));
    }

    private Node loadMap(JavaSoundPhononSettings soundSettings, Node rootNode, Node fpsRootNode, FilterPostProcessor sceneFPP) {

        // Get the env baker, create if it doesn't exist.
        SimplerEnvBaker baker=stateManager.getState(SimplerEnvBaker.class);
        if(baker == null){
            baker=new SimplerEnvBaker();
            baker.setCacheFolder(System.getProperty("user.dir"));
            baker.setAssetCacheFolder("fpstemplate/cache");
            stateManager.attach(baker);
        }

        // Load sky
        Spatial sky=SkyFactory.createSky(assetManager,"fpstemplate/skies/whipple_creek_regional_park_04_4k.hdr",EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        // Bake probe
        baker.bakeEnv(stateManager,assetManager,sky,256,Vector3f.ZERO,probe -> {
            rootNode.addLight(probe);
            fpsRootNode.addLight(probe);
        });

        // Add Background music
        SoundEmitterControl background=new SoundEmitterControl(assetManager,"fpstemplate/sounds/Bogart VGM - Scifi Concentration (looped).f32leS");
        rootNode.addControl(background);
        background.setLooping(true);
        background.setVolume(0.1f);
        background.play();

        BulletAppState bullet=stateManager.getState(BulletAppState.class);

        // Load map
        F3bKey mapk=new F3bKey("fpstemplate/models/world.f3b");
        mapk.usePhysics(new BulletPhysicsLoader()); // enable physics loader
        mapk.useEnhancedRigidbodies(true);

        F3bRuntimeLoader rloader=F3bRuntimeLoader.instance();

        rloader.attachSceneTo(rootNode);
        rloader.attachLightsTo(rootNode);
        rloader.attachPhysicsTo(bullet.getPhysicsSpace());

        Node map=(Node)rloader.load(assetManager,mapk);

        // Set shadowmode
        map.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // Add hit effects to movable rigidbodies
        map.depthFirstTraversal(sx -> {
            RigidBodyControl rb=sx.getControl(RigidBodyControl.class);
            if(rb != null && rb.getMass() > 0){
                sx.addControl(new HitEffectControl(assetManager));
            }
        });

        // Send the scene to the audio engine (for occlusion and reverb)
        Phonon.loadScene(soundSettings,this,map,(sp) -> {
            // Filter: Only static rigidbodies will be used to compute the scene
            RigidBodyControl rb=sp.getControl(RigidBodyControl.class);
            return rb != null && rb.getMass() > 0;
        });

        // Init filters
        ArrayList<Filter> filters=new ArrayList<Filter>();
        filters.addAll(sceneFPP.getFilterList());

        for(Light l:rootNode.getLocalLightList()){
            fpsRootNode.addLight(l);
            // Init shadows
            if(l instanceof DirectionalLight){
                for(int i=0;i < filters.size();i++){
                    Filter f=filters.get(i);
                    if(f instanceof DirectionalLightShadowFilter){
                        DirectionalLightShadowFilter dls=(DirectionalLightShadowFilter)f;
                        dls.setLight((DirectionalLight)l);
                        filters.remove(i);
                        break;
                    }
                }
            }
            // init scattering
            if(l instanceof DirectionalLight){
                for(int i=0;i < filters.size();i++){
                    Filter f=filters.get(i);
                    if(f instanceof LightScatteringFilter){
                        LightScatteringFilter dls=(LightScatteringFilter)f;
                        dls.setLightPosition(((DirectionalLight)l).getDirection().mult(-3000f));
                        filters.remove(i);
                        break;
                    }
                }
            }
        }

        return map;
    }

    @Override
    public void simpleUpdate(float tpf) {

    }

}