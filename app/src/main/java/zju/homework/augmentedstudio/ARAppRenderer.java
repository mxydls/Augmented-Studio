package zju.homework.augmentedstudio;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;

import com.vuforia.Device;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Vuforia;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import zju.homework.augmentedstudio.Activities.ARSceneActivity;
import zju.homework.augmentedstudio.Models.CubeObject;
import zju.homework.augmentedstudio.Shaders.CubeShaders;
import zju.homework.augmentedstudio.Models.MeshObject;
import zju.homework.augmentedstudio.Models.Texture;
import zju.homework.augmentedstudio.Interfaces.ARAppRendererControl;
import zju.homework.augmentedstudio.GL.ARBaseRenderer;
import zju.homework.augmentedstudio.Utils.Util;

/**
 * Created by stardust on 2016/12/12.
 */

public class ARAppRenderer implements GLSurfaceView.Renderer, ARAppRendererControl{


    private static final String LOGTAG = "ARAppRenderer";

    private ARApplicationSession vuforiaAppSession;
    private ARBaseRenderer mSampleAppRenderer;
    private Renderer mRenderer;

    private boolean mIsActive = false;

   private Vector<Texture> mTextures = null;
    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    Texture texture = null;

    // Constants:
    static final float kObjectScale = 3.f;

//    private Teapot mTeapot;

    // Reference to main activity
    private ARSceneActivity mActivity;

    MeshObject cube;

    private boolean ismIsActive;
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mLookatMatrix = new float[]{
            0f, 0f, -10f,
            0f, 0f, 0f,
            0f, 1.0f, 0f
    };

    public ARAppRenderer(ARSceneActivity activity, ARApplicationSession session) {
        mActivity = activity;
        vuforiaAppSession = session;

        mSampleAppRenderer = new ARBaseRenderer(this, mActivity, Device.MODE.MODE_AR,
                false, 0.1f, 100f);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        mRenderer = Renderer.getInstance();

        vuforiaAppSession.onSurfaceCreated();

        mSampleAppRenderer.onSurfaceCreated();
    }

    void initRendering(){
        Log.i(LOGTAG, "initRendering");



        cube = new CubeObject();

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        for(Texture t : mTextures){
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = Util.createProgramFromShaderSrc(CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);
        if( shaderProgramID > 0 ){
            GLES20.glUseProgram(shaderProgramID);
            texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                    "texSampler2D");
            vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                    "vertexPosition");
            textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                    "vertexTexCoord");
            mvpMatrixHandle =GLES20.glGetUniformLocation(shaderProgramID,
                    "modelViewProjectionMatrix");
        }


    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call function to update rendering when render surface
        // parameters have changed:
        mActivity.updateRendering();

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mSampleAppRenderer.onConfigurationChanged(mIsActive);

        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / (float) height;
        Matrix.perspectiveM(mProjectionMatrix, 0, 45, ratio, 0.1f, 100f);

        // Call function to initialize rendering:
        initRendering();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if( !ismIsActive )
            return;
        mSampleAppRenderer.render();
    }

    @Override
    public void renderFrame(State state, float[] projectionMatrix) {

//        Log.i(LOGTAG, "renderFrame");

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        float[] modelViewMatrix = new float[16];
        float[] mvpMatrix = new float[16];

        Matrix.setLookAtM(modelViewMatrix, 0,
                mLookatMatrix[0], mLookatMatrix[1], mLookatMatrix[2],
                mLookatMatrix[3], mLookatMatrix[4], mLookatMatrix[5],
                mLookatMatrix[6], mLookatMatrix[7], mLookatMatrix[8]);

//        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);         // use for AR mos
        Matrix.multiplyMM(mvpMatrix, 0, mProjectionMatrix, 0, modelViewMatrix, 0);

        GLES20.glUseProgram(shaderProgramID);

        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                false, 0, cube.getVertices());

        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT,
                false, 0, cube.getTexCoords());

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                mTextures.get(0).mTextureID[0]);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                mvpMatrix, 0);
        GLES20.glUniform1i(texSampler2DHandle, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                cube.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                cube.getIndices());

        GLES20.glDisableVertexAttribArray(vertexHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        mRenderer.end();
    }

    public boolean getActive() {
        return ismIsActive;
    }

    public void setActive(boolean ismIsActive) {
        this.ismIsActive = ismIsActive;

        if ( mIsActive ){
//            mSampleAppRenderer.configureVideoBackground();
        }
    }

    public Vector<Texture> getTextures() {
        return mTextures;
    }

    public void setTextures(Vector<Texture> mTextures) {
        this.mTextures = mTextures;
    }


//    private float oldX;
//    private float oldY;
//    private final float ROTATE_SPEED = 0.4f;		//Proved to be good for normal rotation ( NEW )
//    private final float TRANSFORM_SPEED = 0.02f;
//    private boolean isTouching = false;
//
//    private int selectIndex = -1;
//
//    public boolean handleTouchEvent(MotionEvent event){
//        float x = event.getX();
//        float y = event.getY();
//
//        //If a touch is moved on the screen
//        if(event.getAction() == MotionEvent.ACTION_MOVE) {
//            //Calculate the change
//            isTouching = true;
//            float dx = x - oldX;
//            float dy = y - oldY;
//            //Define an upper area of 10% on the screen
//            int leftArea = width / 4;
//
//            //Zoom in/out if the touch move has been made in the upper
//            if( selectIndex >= 0 && selectIndex < models.size()  ){
//
//                if( mode.equals("Rotate") ) {
//                    float[] rotation = models.get(selectIndex).getRotation();
//                    rotation[0] += dy * ROTATE_SPEED;
//                    rotation[1] += dx * ROTATE_SPEED;
//                }else {
//                    if(x < leftArea) {
//                        float[] position = models.get(selectIndex).getPosition();
//                        position[2] -= dy * TRANSFORM_SPEED / 2;
//                        //Rotate around the axis otherwise
//                    } else {
//                        float[] position = models.get(selectIndex).getPosition();
//                        position[0] += dx * TRANSFORM_SPEED;
//                        position[1] += -dy * TRANSFORM_SPEED;
//                    }
//                }
//
//            }else if ( selectIndex == models.size() ){		// camera
//
//                if( mode.equals("Rotate") ) {
//
//                }else {
//
//                    if( x < leftArea ){
//                        mLookatMatrix[2] -= dy * TRANSFORM_SPEED;
//                        mLookatMatrix[5] -= dy * TRANSFORM_SPEED;
//                    }else{
//                        mLookatMatrix[0] -= dx * TRANSFORM_SPEED;
//                        mLookatMatrix[3] -= dx * TRANSFORM_SPEED;
//
//                        mLookatMatrix[1] += dy * TRANSFORM_SPEED;
//                        mLookatMatrix[4] += dy * TRANSFORM_SPEED;
//                    }
//                }
//            }
//
//            if( selectIndex == 0 ){
//                updateLight();
//            }
//
//
//            //A press on the screen
//        } else if(event.getAction() == MotionEvent.ACTION_UP) {
////            mRenderer.isTouching = false;
//            isTouching = false;
//        }
//
//        //Remember the values
//        oldX = x;
//        oldY = y;
//
//        return true;
//    }
}
