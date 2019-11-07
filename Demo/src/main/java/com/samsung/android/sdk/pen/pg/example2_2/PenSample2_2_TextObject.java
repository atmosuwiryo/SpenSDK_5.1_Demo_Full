package com.samsung.android.sdk.pen.pg.example2_2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.SpenSettingPenInfo;
import com.samsung.android.sdk.pen.SpenSettingTextInfo;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenObjectImage;
import com.samsung.android.sdk.pen.document.SpenObjectTextBox;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.document.textspan.SpenFontSizeSpan;
import com.samsung.android.sdk.pen.document.textspan.SpenLineSpacingParagraph;
import com.samsung.android.sdk.pen.document.textspan.SpenTextParagraphBase;
import com.samsung.android.sdk.pen.document.textspan.SpenTextSpanBase;
import com.samsung.android.sdk.pen.engine.SpenColorPickerListener;
import com.samsung.android.sdk.pen.engine.SpenContextMenu;
import com.samsung.android.sdk.pen.engine.SpenContextMenuItemInfo;
import com.samsung.android.sdk.pen.engine.SpenControlBase;
import com.samsung.android.sdk.pen.engine.SpenControlShape;
import com.samsung.android.sdk.pen.engine.SpenControlTextBox;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTextChangeListener;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;
import com.samsung.android.sdk.pen.engine.SpenLongPressListener;
import com.samsung.android.sdk.pen.pg.tool.SDKUtils;
import com.samsung.android.sdk.pen.settingui.SpenSettingPenLayout;
import com.samsung.android.sdk.pen.settingui.SpenSettingTextLayout;
import com.samsung.spensdk3.example.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PenSample2_2_TextObject extends Activity {

    private final int MODE_PEN = 0;
    private final int MODE_IMG_OBJ = 1;
    private final int MODE_TEXT_OBJ = 2;

    private Context mContext;
    private SpenNoteDoc mSpenNoteDoc;
    private SpenPageDoc mSpenPageDoc;
    private SpenSurfaceView mSpenSurfaceView;
    private FrameLayout mSettingView;
    private SpenSettingPenLayout mPenSettingView;
    private SpenSettingTextLayout mTextSettingView;

    private ImageView mPenBtn;
    private ImageView mImgObjBtn;
    private ImageView mTextObjBtn;

    private Rect mScreenRect;
    private int mMode = MODE_PEN;
    private int mToolType = SpenSurfaceView.TOOL_SPEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_object);
        mContext = this;

        // Initialize Spen
        boolean isSpenFeatureEnabled = false;
        Spen spenPackage = new Spen();
        try {
            spenPackage.initialize(this);
            isSpenFeatureEnabled = spenPackage.isFeatureEnabled(Spen.DEVICE_PEN);
        } catch (SsdkUnsupportedException e) {
            if (SDKUtils.processUnsupportedException(this, e) == true) {
                return;
            }
        } catch (Exception e1) {
            Toast.makeText(mContext, "Cannot initialize Spen.", Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
            finish();
        }

        RelativeLayout spenViewLayout = (RelativeLayout) findViewById(R.id.spenViewLayout);

        // Create PenSettingView
        mPenSettingView = new SpenSettingPenLayout(getApplicationContext(), "", spenViewLayout);

        // Create TextSettingView
        HashMap<String, String> hashMapFont = new HashMap<String, String>();
        hashMapFont.put("Droid Sans Georgian", "/system/fonts/DroidSansGeorgian.ttf");
        hashMapFont.put("Droid Serif", "/system/fonts/DroidSerif-Regular.ttf");
		hashMapFont.put("Droid Sans", "/system/fonts/DroidSans.ttf");
		hashMapFont.put("Droid Sans Mono", "/system/fonts/DroidSansMono.ttf");
        mTextSettingView = (SpenSettingTextLayout) findViewById(R.id.settingTextLayout);
        mTextSettingView.initialize("", hashMapFont, spenViewLayout);

        mSettingView = (FrameLayout) findViewById(R.id.settingView);
        mSettingView.addView(mPenSettingView);

        // Create SpenSurfaceView
        mSpenSurfaceView = new SpenSurfaceView(mContext);
        if (mSpenSurfaceView == null) {
            Toast.makeText(mContext, "Cannot create new SpenSurfaceView.", Toast.LENGTH_SHORT).show();
            finish();
        }
        mSpenSurfaceView.setToolTipEnabled(true);
        spenViewLayout.addView(mSpenSurfaceView);
        mPenSettingView.setCanvasView(mSpenSurfaceView);
        mTextSettingView.setCanvasView(mSpenSurfaceView);

        // Get the dimension of the device screen.
        Display display = getWindowManager().getDefaultDisplay();
        mScreenRect = new Rect();
        display.getRectSize(mScreenRect);
        // Create SpenNoteDoc
        try {
            mSpenNoteDoc = new SpenNoteDoc(mContext, mScreenRect.width(), mScreenRect.height());
        } catch (IOException e) {
            Toast.makeText(mContext, "Cannot create new NoteDoc.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
        // Add a Page to NoteDoc, get an instance, and set it to the member variable.
        mSpenPageDoc = mSpenNoteDoc.appendPage();
        mSpenPageDoc.setBackgroundColor(0xFFD6E6F5);
        mSpenPageDoc.clearHistory();
        // Set PageDoc to View
        mSpenSurfaceView.setPageDoc(mSpenPageDoc, true);

        initSettingInfo();
        // Register the listener
        mSpenSurfaceView.setTouchListener(mPenTouchListener);
        mSpenSurfaceView.setPreTouchListener(onPreTouchSurfaceViewListener);
        mSpenSurfaceView.setColorPickerListener(mColorPickerListener);
        mSpenSurfaceView.setTextChangeListener(mTextChangeListener);
        mSpenSurfaceView.setLongPressListener(onLongPressListenner);

        // Set a button
        mPenBtn = (ImageView) findViewById(R.id.penBtn);
        mPenBtn.setOnClickListener(mPenBtnClickListener);

        mImgObjBtn = (ImageView) findViewById(R.id.imgObjBtn);
        mImgObjBtn.setOnClickListener(mImgObjBtnClickListener);

        mTextObjBtn = (ImageView) findViewById(R.id.textObjBtn);
        mTextObjBtn.setOnClickListener(mTextObjBtnClickListener);

        selectButton(mPenBtn);

        if (isSpenFeatureEnabled == false) {
            mToolType = SpenSurfaceView.TOOL_FINGER;
            Toast.makeText(mContext, "Device does not support Spen. \n You can draw stroke by finger",
                    Toast.LENGTH_SHORT).show();
        } else {
            mToolType = SpenSurfaceView.TOOL_SPEN;
        }
        mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_STROKE);
    }

    private void initSettingInfo() {

        int mCanvasWidth = mScreenRect.width();

        if (mSpenSurfaceView != null) {
            if (mSpenSurfaceView.getCanvasWidth() < mSpenSurfaceView.getCanvasHeight()) {
                mCanvasWidth = mSpenSurfaceView.getCanvasWidth();
            } else {
                mCanvasWidth = mSpenSurfaceView.getCanvasHeight();
            }
            if (mCanvasWidth == 0) {
                mCanvasWidth = mScreenRect.width();
            }
        }

        // Initialize Pen settings
        SpenSettingPenInfo penInfo = new SpenSettingPenInfo();
        penInfo.color = Color.BLUE;
        penInfo.size = 10;
        mSpenSurfaceView.setPenSettingInfo(penInfo);
        mPenSettingView.setInfo(penInfo);

        // Initialize text settings
        SpenSettingTextInfo textInfo = new SpenSettingTextInfo();
//        textInfo.size = Math.round(18 * mCanvasWidth / 360);
        mSpenSurfaceView.setTextSettingInfo(textInfo);
        mTextSettingView.setInfo(textInfo);
    }

    private SpenLongPressListener onLongPressListenner = new SpenLongPressListener(){
        @Override
        public void onLongPressed(MotionEvent event){
            enableButton(true);
        }
    };

    private SpenTouchListener onPreTouchSurfaceViewListener = new SpenTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            // TODO Auto-generated method stub.
            Log.e("HZ", "HZ onPreTouchSurfaceViewListener: " + event.getAction());
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    enableButton(false);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    enableButton(true);
                    break;
            }
            return false;
        }
    };

    private final SpenTouchListener mPenTouchListener = new SpenTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP && event.getToolType(0) == mToolType) {
                // Check if the control is created.
                SpenControlBase control = mSpenSurfaceView.getControl();
                if (control == null) {
                    // When Pen touches the display while it is in Add ObjectImage mode
                    if (mMode == MODE_IMG_OBJ) {
                        // Set a bitmap file to ObjectImage.
                        SpenObjectImage imgObj = new SpenObjectImage();
                        Bitmap imageBitmap = BitmapFactory.decodeResource(mContext.getResources(),
                                R.drawable.ic_launcher);
                        imgObj.setImage(imageBitmap);

                        // Set the location to insert ObjectImage and add it to PageDoc.
                        PointF canvasPos = getCanvasPoint(event);
                        RectF rect = new RectF(canvasPos.x - (imageBitmap.getWidth() / 2), canvasPos.y
                                - (imageBitmap.getHeight() / 2), canvasPos.x + (imageBitmap.getWidth() / 2),
                                canvasPos.y + (imageBitmap.getHeight() / 2));
                        imgObj.setRect(rect, true);
                        mSpenPageDoc.appendObject(imgObj);
                        mSpenSurfaceView.update();

                        imageBitmap.recycle();
                        return true;
                        // When Pen touches the display while it is in Add ObjectTextBox mode
                    } else if (mSpenSurfaceView.getToolTypeAction(mToolType) == SpenSurfaceView.ACTION_TEXT) {
                        // Set the location to insert ObjectTextBox and add it to PageDoc.
                        SpenObjectTextBox textObj = new SpenObjectTextBox();
                        PointF canvasPos = getCanvasPoint(event);
                        float x = canvasPos.x;
                        float y = canvasPos.y;
                        float textBoxHeight = getTextBoxDefaultHeight(textObj);
                        if ((y + textBoxHeight) > mSpenPageDoc.getHeight()) {
                            y = mSpenPageDoc.getHeight() - textBoxHeight;
                        }
                        RectF rect = new RectF(x, y, x + 350, y + textBoxHeight);
                        textObj.setRect(rect, true);
                        mSpenPageDoc.appendObject(textObj);
                        mSpenPageDoc.selectObject(textObj);
                        mSpenSurfaceView.update();
                    }
                }
            }
            return false;
        }
    };

    private float getTextBoxDefaultHeight(SpenObjectTextBox textBox) {
        if (textBox == null) {
            return 0;
        }

        float height = 0, lineSpacing = 0, lineSpacePercent = 1.3f;
        float margin = textBox.getTopMargin() + textBox.getBottomMargin();

        ArrayList<SpenTextParagraphBase> pInfo = textBox.getTextParagraph();
        if (pInfo != null) {
            for (SpenTextParagraphBase info : pInfo) {
                if (info instanceof SpenLineSpacingParagraph) {
                    if (((SpenLineSpacingParagraph) info).getLineSpacingType() ==
                            SpenLineSpacingParagraph.TYPE_PERCENT) {
                        lineSpacePercent = ((SpenLineSpacingParagraph) info).getLineSpacing();
                    } else if (((SpenLineSpacingParagraph) info).getLineSpacingType() ==
                            SpenLineSpacingParagraph.TYPE_PIXEL) {
                        lineSpacing = ((SpenLineSpacingParagraph) info).getLineSpacing();
                    }
                }
            }
        }

        if (lineSpacing != 0){
            height = lineSpacing + margin;
        } else {
            float fontSize = mSpenPageDoc.getWidth()/20;
            ArrayList<SpenTextSpanBase> sInfo =
                    textBox.findTextSpan(textBox.getCursorPosition(), textBox.getCursorPosition());
            if (sInfo != null) {
                for (SpenTextSpanBase info : sInfo) {
                    if (info instanceof SpenFontSizeSpan) {
                        fontSize = ((SpenFontSizeSpan) info).getSize();
                        break;
                    }
                }
            }
            height = fontSize * lineSpacePercent;
        }

        return height;
    }

    private PointF getCanvasPoint(MotionEvent event) {
        float panX = mSpenSurfaceView.getPan().x;
        float panY = mSpenSurfaceView.getPan().y;
        float zoom = mSpenSurfaceView.getZoomRatio();
        return new PointF(event.getX() / zoom + panX, event.getY() / zoom + panY);
    }

    private final OnClickListener mPenBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mSpenSurfaceView.closeControl();

            // When Spen is in stroke (pen) mode
            if (mSpenSurfaceView.getToolTypeAction(mToolType) == SpenSurfaceView.ACTION_STROKE) {
                // If PenSettingView is open, close it.
                if (mPenSettingView.isShown()) {
                    mPenSettingView.setVisibility(View.GONE);
                    // If PenSettingView is not open, open it.
                } else {
                    mPenSettingView.setViewMode(SpenSettingPenLayout.VIEW_MODE_NORMAL);
                    mPenSettingView.setVisibility(View.VISIBLE);
                }
                // If Spen is not in stroke (pen) mode, change it to stroke mode.
            } else {
                mMode = MODE_PEN;
                selectButton(mPenBtn);
                mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_STROKE);
            }
        }
    };

    private final OnClickListener mImgObjBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mSpenSurfaceView.closeControl();

            mMode = MODE_IMG_OBJ;
            selectButton(mImgObjBtn);
            mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_NONE);
        }
    };

    private final OnClickListener mTextObjBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mSpenSurfaceView.closeControl();
            closeSettingView();
            mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_TEXT);
            mMode = MODE_TEXT_OBJ;
            selectButton(mTextObjBtn);
        }
    };

    private final SpenColorPickerListener mColorPickerListener = new SpenColorPickerListener() {
        @Override
        public void onChanged(int color, int x, int y) {
            // Set the color from the Color Picker to the setting view.
            if (mPenSettingView != null) {
                if (mMode == MODE_PEN) {
                    SpenSettingPenInfo penInfo = mPenSettingView.getInfo();
                    penInfo.color = color;
                    mPenSettingView.setInfo(penInfo);
                } else if (mMode == MODE_TEXT_OBJ) {
                    SpenSettingTextInfo textInfo = mSpenSurfaceView.getTextSettingInfo();
                    textInfo.color = color;
                    mTextSettingView.setInfo(textInfo);
                }
            }
        }
    };

    // text context menu
    public static final int TEXT_CONTEXT_MENU_SELECT_ALL = 0;

    public static final int TEXT_CONTEXT_MENU_CUT = 1;
    public static final int TEXT_CONTEXT_MENU_COPY = 2;
    public static final int TEXT_CONTEXT_MENU_PASTE = 3;
    public static final int TEXT_CONTEXT_MENU_CLIPBOARD = 4;
    public static final int TEXT_CONTEXT_MENU_SHARE = 5;
    public static final int TEXT_CONTEXT_MENU_DICTIONARY = 6;
    public static final int TEXT_CONTEXT_MENU_TRANSLATE = 7;


    private ArrayList<SpenContextMenuItemInfo> mTextContextMenuList = new ArrayList<SpenContextMenuItemInfo>();
    private SpenContextMenu.ContextMenuListener mSelectContextMenuListener = new SpenContextMenu.ContextMenuListener() {

        @Override
        public void onSelected(int action) {

            SpenControlBase base = mSpenSurfaceView.getControl();
            if (base instanceof SpenControlTextBox) {
                SpenControlTextBox controlTextBox = (SpenControlTextBox) base;

                switch (action) {
                    case TEXT_CONTEXT_MENU_SELECT_ALL:
                        controlTextBox.selectAllText();
                        break;
                    case TEXT_CONTEXT_MENU_COPY:
                        //int cursorPosition = getSavedTextCursorPosition();
                        //controlTextBox.setSelection(cursorPosition, cursorPosition);
                        break;
                    case TEXT_CONTEXT_MENU_CUT:
                        controlTextBox.removeText();
                        break;
                }
            }

            if (mContextMenu != null) {
                mContextMenu.hide();
            }
        }
    };

    public void addTextContextMenuItem(int id, Drawable icon, String name) {
        mTextContextMenuList.add(new SpenContextMenuItemInfo(id, icon, name, true));
    }

        SpenContextMenu mContextMenu = null;
    SpenTextChangeListener mTextChangeListener = new SpenTextChangeListener() {

        @Override
        public boolean onSelectionChanged(int arg0, int arg1) {
            Log.i("Debug", "onSelectionChanged, " + arg0 + ", " + arg1);

            mTextContextMenuList.clear();
            addTextContextMenuItem(TEXT_CONTEXT_MENU_CUT,
                    getDrawable(R.drawable.ic_launcher), "Cut");

            addTextContextMenuItem(TEXT_CONTEXT_MENU_PASTE,
                    getDrawable(R.drawable.ic_launcher), "Paste");


            mContextMenu = new SpenContextMenu(mContext, mSpenSurfaceView, mTextContextMenuList, mSelectContextMenuListener);

            SpenControlBase mControl = mSpenSurfaceView.getControl();
            if (mControl != null) {
                if (mControl instanceof SpenControlTextBox) {
                    ((SpenControlTextBox) mControl).setTextSelectionContextMenu(mContextMenu);
                }
                if (mControl instanceof SpenControlShape) {
                    ((SpenControlShape) mControl).setTextSelectionContextMenu(mContextMenu);
                }
            }

            return true;
        }

        @Override
        public void onMoreButtonDown(SpenObjectTextBox arg0) {
        }

        @Override
        public void onChanged(SpenSettingTextInfo info, int state) {
            if (mTextSettingView != null) {
                if (state == CONTROL_STATE_SELECTED) {
                    mTextSettingView.setInfo(info);
                }
            }
        }

        @Override
        public void onFocusChanged(boolean gainFocus) {
            if (mTextSettingView != null) {
                if (gainFocus == true) {
                    // show text setting
                    mTextSettingView.setVisibility(View.VISIBLE);
                    mMode = MODE_TEXT_OBJ;
                } else {
                    // hide text setting
                    mTextSettingView.setVisibility(View.GONE);
                    mMode = MODE_PEN;
                }
            }
        }
    };

    private void enableButton(boolean isEnable) {
        mPenBtn.setEnabled(isEnable);
        mImgObjBtn.setEnabled(isEnable);
        mTextObjBtn.setEnabled(isEnable);
    }

    private void selectButton(View v) {
        // Enable or disable the button according to the current mode.
        mPenBtn.setSelected(false);
        mImgObjBtn.setSelected(false);
        mTextObjBtn.setSelected(false);

        v.setSelected(true);

        closeSettingView();
    }

    private void closeSettingView() {
        // Close all the setting views.
        mPenSettingView.setVisibility(SpenSurfaceView.GONE);
        mTextSettingView.setVisibility(SpenSurfaceView.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPenSettingView != null) {
            mPenSettingView.close();
        }
        if (mTextSettingView != null) {
            mTextSettingView.close();
        }

        if (mSpenSurfaceView != null) {
            mSpenSurfaceView.closeControl();
            mSpenSurfaceView.close();
            mSpenSurfaceView = null;
        }

        if (mSpenNoteDoc != null) {
            try {
                mSpenNoteDoc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSpenNoteDoc = null;
        }
    };
}