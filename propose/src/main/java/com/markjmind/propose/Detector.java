package com.markjmind.propose;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * <br>捲土重來<br>
 *
 * @author 오재웅(JaeWoong-Oh)
 * @email markjmind@gmail.com
 * @since 2016-03-28
 */
public class Detector extends GestureDetector.SimpleOnGestureListener implements PointEvent.SyncPointListener {

    protected PointEvent pointEventX, pointEventY;

    private Hashtable<Integer, MotionsInfo> motionMap = new Hashtable<>();
    private ActionState action;
    private boolean isFirstTouch;
    private float density;

    private DetectListener detectListener;

    protected int horizontalPriority = Motion.RIGHT;
    protected int verticalPriority = Motion.UP;

    protected Detector(float density, DetectListener detectListener){
        this.density = density;

        pointEventX = new PointEvent(Motion.LEFT, Motion.RIGHT);
        pointEventY = new PointEvent(Motion.UP, Motion.DOWN);
        action = new ActionState();
        this.detectListener = detectListener;
        reset();
    }

    protected void reset(){
        isFirstTouch = true;
        action.setAction(ActionState.STOP);
    }


    protected void addMotion(int direction, Motion motion){
        MotionsInfo info = new MotionsInfo();
        if (motionMap.containsKey(direction)) {
            info = motionMap.get(direction);
        }
        if(direction == Motion.LEFT || direction == Motion.RIGHT) {
            motion.setPointEvent(pointEventX);
        }else if(direction == Motion.UP || direction == Motion.DOWN){
            motion.setPointEvent(pointEventY);
        }
        info.add(motion);
        motionMap.put(direction, info);
    }

    protected int getActionState() {
        return this.action.getAction();
    }


    /****************************************** Gesture ****************************************/

    @Override
    public boolean onDown(MotionEvent event) {
        isFirstTouch = true;
        pointEventX.setCurrRaw(event.getRawX(), event.getEventTime());
        pointEventX.setAcceleration(0f);
        pointEventY.setCurrRaw(event.getRawY(), event.getEventTime());
        pointEventY.setAcceleration(0f);
        return super.onDown(event);
    }

    public boolean onUp(MotionEvent event){
        if(action.getAction() == ActionState.SCROLL){
            action.setAction(ActionState.STOP);
        }
        return false;
    }


    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        boolean result = true;
        Log.i("Detector", "Tap:1");
        result = tapUp(pointEventX, horizontalPriority);
        result = tapUp(pointEventY, verticalPriority) || result;
        return result;
    }

    private boolean tapUp(PointEvent pointEvent, int priority){
        boolean result = false;
        int direction = pointEvent.getDirection();
        if(direction==0){
            direction = priority;
        }
        MotionsInfo info;
        if(direction != Motion.NONE && (info = motionMap.get(direction)) != null) {
            for (Motion motion : info.motions) {
                result = motion.tap() || result;
            }
        }
        return result;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        boolean result = false;
        if(action.getAction() != ActionState.ANIMATION){
            action.setAction(ActionState.SCROLL);

            if(isFirstTouch){ //최초 스크롤시
                isFirstTouch = false;
                pointEventX.setRaw(e2.getRawX());
                pointEventY.setRaw(e2.getRawY());
                pointEventX.setPreRaw(e2.getRawX());
                pointEventY.setPreRaw(e2.getRawX());
                result = true;
            } else {
                result = scroll(e2.getRawX(), pointEventX);
                result = scroll(e2.getRawY(), pointEventY) || result;

                result = detectListener.onScroll(pointEventX, pointEventY) || result;
            }
            pointEventX.setCurrRawAndAccel(e2.getRawX(), e2.getEventTime(), density);
            pointEventY.setCurrRawAndAccel(e2.getRawY(), e2.getEventTime(), density);
        }
        return result;
    }

    private boolean scroll(float raw, PointEvent pointEvent){
        float diff = raw - pointEvent.getRaw();
        pointEvent.setRaw(raw);
        int direction = pointEvent.getDirection(diff);

        boolean result = false;
        MotionsInfo info;
        if(direction != Motion.NONE && (info = motionMap.get(direction)) != null) {
            for (Motion motion : info.motions) {
                result = motion.moveDistance(Math.abs(pointEvent.getPoint() + diff)) || result;
            }
        }else{
            pointEvent.setPoint(0);
        }
        return result;
    }



    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.e("Detector", "flingX:"+pointEventX.getAcceleration()+ " : "+velocityX/1000);
        boolean result = false;
        if(action.getAction() == ActionState.SCROLL){
            action.setAction(ActionState.FlING);
        }
        return result;
    }

    @Override
    public void syncPoint(float distance) {
//        pointEvent.setPoint(distance);
    }


    /****************************************** interface and inner class ****************************************/

    interface DetectListener {
        boolean onScroll(PointEvent pointEventX, PointEvent pointEventY);
        boolean onFling(PointEvent pointEventX, PointEvent pointEventY);
    }

    class MotionsInfo {
        protected long maxDuration=0;
        protected ArrayList<Motion> motions;
        protected MotionsInfo(){
            motions = new ArrayList<>();
        }

        protected void add(Motion motion){
            if(maxDuration < motion.getTotalDuration()){
                maxDuration = motion.getTotalDuration();
            }
            motions.add(motion);
        }
    }
}
