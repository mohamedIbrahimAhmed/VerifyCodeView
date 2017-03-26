package com.github.irvingryan;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.IntDef;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.github.irvingryan.utils.UIUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.text.TextUtils.isEmpty;
import static android.view.KeyEvent.KEYCODE_0;
import static android.view.KeyEvent.KEYCODE_9;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_ENTER;

/**
 * Created by yanwentao on 2016/10/20 0020.
 */

public class VerifyCodeView extends View {

    private static final String TAG = "VerifyCodeView";


    // Default values
    private static final int DEFAULT_VERIFICATION_CODE_LENGTH = 4;
    private static final int DEFAULT_TEXT_COLOR = Color.CYAN;
    private static final Typeface DEFAULT_TYPE_FACE = Typeface.DEFAULT;
    private static final int DEFAULT_BLANK_LINE_STROKE = 5;
    private static final int INPUT_NO_LINE = 0;
    private static final int INPUT_LINE_UNDER_TEXT = 1;
    private static final int DEFAULT_INPUT_TYPE = INPUT_NO_LINE;
    private static final int DEFAULT_ANIMATION = R.anim.invalid_input;
    private static final boolean DEFAULT_ALLOW_ANIMATION = true;
    private static final boolean DEFAULT_REMOVE_WHEN_ERROR = false;


    private int mHeight;


    private final StringBuilder codeBuilder = new StringBuilder();
    private final int vcBlankLineStroke;
    private final int vcVerificationCodeLength;

    private final Paint vcLinePaint;
    private final Paint vcTextPaint;
    private final boolean vcRemoveWhenError;
    private VerificationListener listener;
    private final int vcTextColor;
    private final Typeface vcTypeface;
    private final int vcLineColor;
    private final boolean vcAllowAnimation;
    private int vcDefaultTextSize;
    private final int vcTextSize;
    private final Point[] solidPoints;
    private final Animation animation;
    private String mVerificationCode; // verification mVerificationCode to verify


    @IntDef({INPUT_NO_LINE, INPUT_LINE_UNDER_TEXT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LineStyle {
    }

    @LineStyle
    private int vcInputStyle = DEFAULT_INPUT_TYPE;

    public VerifyCodeView(Context context) {
        this(context, null);
    }

    public VerifyCodeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public VerifyCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        if (attrs == null) {
            vcRemoveWhenError = DEFAULT_REMOVE_WHEN_ERROR;
            vcBlankLineStroke = DEFAULT_BLANK_LINE_STROKE;
            vcLineColor = DEFAULT_TEXT_COLOR;
            vcTextColor = DEFAULT_TEXT_COLOR;
            vcVerificationCodeLength = DEFAULT_VERIFICATION_CODE_LENGTH;
            vcTypeface = DEFAULT_TYPE_FACE;
            vcTextSize = 0;
            vcAllowAnimation = DEFAULT_ALLOW_ANIMATION;
            animation = AnimationUtils.loadAnimation(context, DEFAULT_ANIMATION);


        } else {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerifyCodeView);


            vcRemoveWhenError = typedArray.getBoolean(R.styleable.VerifyCodeView_vcRemoveWhenError, DEFAULT_REMOVE_WHEN_ERROR);
            vcTextSize = typedArray.getDimensionPixelSize(R.styleable.VerifyCodeView_vcTextSize, 0);
            vcAllowAnimation = typedArray.getBoolean(R.styleable.VerifyCodeView_vcAllowAnimation, DEFAULT_ALLOW_ANIMATION);

            vcTextColor = typedArray.getColor(R.styleable.VerifyCodeView_vcTextColor, DEFAULT_TEXT_COLOR);
            vcLineColor = typedArray.getColor(R.styleable.VerifyCodeView_vcLineColor, DEFAULT_TEXT_COLOR);
            vcVerificationCodeLength = typedArray.getInt(R.styleable.VerifyCodeView_vcCodeLength, DEFAULT_VERIFICATION_CODE_LENGTH);
            vcBlankLineStroke = typedArray.getDimensionPixelSize(R.styleable.VerifyCodeView_vcLineWidth, DEFAULT_BLANK_LINE_STROKE);

            String font = typedArray.getString(R.styleable.VerifyCodeView_vcFont);
            if (isEmpty(font)) vcTypeface = DEFAULT_TYPE_FACE;
            else vcTypeface = Typeface.createFromAsset(context.getAssets(), font);


            switch (typedArray.getInt(R.styleable.VerifyCodeView_vcLineStyle, INPUT_NO_LINE)) {
                case INPUT_NO_LINE:
                    vcInputStyle = INPUT_NO_LINE;
                    break;

                case INPUT_LINE_UNDER_TEXT:
                    vcInputStyle = INPUT_LINE_UNDER_TEXT;
                    break;
            }

            animation = AnimationUtils.loadAnimation(context, typedArray.getResourceId(R.styleable.VerifyCodeView_vcAnimation, DEFAULT_ANIMATION));
            typedArray.recycle();
        }


        solidPoints = new Point[vcVerificationCodeLength];

        for (int i = 0; i < vcVerificationCodeLength; i++) {
            solidPoints[i] = new Point();
        }


        vcLinePaint = new Paint();
        vcLinePaint.setColor(vcLineColor);
        vcLinePaint.setAntiAlias(true);
        vcLinePaint.setStrokeWidth(vcBlankLineStroke);

        vcTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vcTextPaint.setColor(vcTextColor);
        vcTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        vcTextPaint.setTextAlign(Paint.Align.CENTER);
        vcTextPaint.setTypeface(vcTypeface);


        setFocusableInTouchMode(true);

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        requestFocus();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(this, InputMethodManager.SHOW_FORCED);
        }
        return true;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        //define keyboard to number keyboard
        BaseInputConnection fic = new BaseInputConnection(this, false);
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_CLASS_NUMBER;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NEXT;
        return fic;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KEYCODE_DEL && codeBuilder.length() > 0) {
            codeBuilder.deleteCharAt(codeBuilder.length() - 1);
            invalidate();

        } else if (keyCode >= KEYCODE_0 && keyCode <= KEYCODE_9 && codeBuilder.length() < vcVerificationCodeLength) {
            codeBuilder.append(keyCode - 7);
            invalidate();
        }


        if (codeBuilder.length() >= vcVerificationCodeLength || keyCode == KEYCODE_ENTER) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getWindowToken(), 0);


            // check auto verify
            if (!isEmpty(mVerificationCode)) {
                if (mVerificationCode.equalsIgnoreCase(codeBuilder.toString())) {
                    if (listener != null) listener.onVerificationSuccess();
                } else {
                    animateInvalid();
                    if (listener != null) listener.onVerificationFail();

                    if (vcRemoveWhenError) {
                        clearText();
                    }
                }


            }

        }

        return super.onKeyDown(keyCode, event);
    }


    public void clearText() {
        codeBuilder.setLength(0);
        invalidate();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.AT_MOST) {
            mWidth = UIUtils.getWidth(getContext()) * 2 / 3;
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            mHeight = UIUtils.getWidth(getContext()) / 2;
        }

        //calculate line's length
        int vcBlankLineSpace = mWidth / (4 * vcVerificationCodeLength - 1);
        vcDefaultTextSize = mWidth / (4 * vcVerificationCodeLength - 1) * 3;

        if (vcTextPaint != null) vcTextPaint.setTextSize(vcDefaultTextSize + vcTextSize);


        for (int i = 1; i <= vcVerificationCodeLength; i++) {
            final int index = i - 1;
            solidPoints[index].x = (index) * vcBlankLineSpace + (index) * vcDefaultTextSize;
            solidPoints[index].y = (index) * vcBlankLineSpace + i * vcDefaultTextSize;

        }


        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLine(canvas);
    }


    private void drawLine(Canvas canvas) {
        if (codeBuilder == null) return;

        final int inputLength = codeBuilder.length();

        final Paint.FontMetricsInt fontMetricsInt = vcTextPaint.getFontMetricsInt();

        //text's vertical center is view's center
        final int baseLine = mHeight / 2 + (fontMetricsInt.bottom - fontMetricsInt.top) / 2 - fontMetricsInt.bottom;


        final int mLinePosY;

        if (vcInputStyle == INPUT_NO_LINE) {
            mLinePosY = mHeight / 2;
            for (int i = 0; i < vcVerificationCodeLength; i++) {
                if (inputLength > i) {
                    canvas.drawText(codeBuilder.toString(), i, i + 1, solidPoints[i].y - vcDefaultTextSize / 2, baseLine, vcTextPaint);
                } else {
                    canvas.drawLine(solidPoints[i].x, mLinePosY, solidPoints[i].y, mLinePosY, vcLinePaint);
                }
            }


        } else {

            mLinePosY = baseLine + vcBlankLineStroke;

            for (int i = 0; i < vcVerificationCodeLength; i++) {
                if (inputLength > i) {
                    canvas.drawText(codeBuilder.toString(), i, i + 1, solidPoints[i].y - vcDefaultTextSize / 2, baseLine, vcTextPaint);
                }
                canvas.drawLine(solidPoints[i].x, mLinePosY, solidPoints[i].y, mLinePosY, vcLinePaint);
            }


        }


    }


    /**
     * get verify mVerificationCode string
     *
     * @return mVerificationCode
     */
    public String getText() {
        return codeBuilder != null ? codeBuilder.toString() : "";
    }


    public interface VerificationListener {
        void onVerificationSuccess();

        void onVerificationFail();
    }


    public void setListener(VerificationListener listener) {
        this.listener = listener;
    }

    public void setVerificationCode(String code) {
        this.mVerificationCode = code;
    }


    public void validate(String codeToValidate) {
        if (isEmpty(codeToValidate)) {
            animateInvalid();
            if (listener != null) listener.onVerificationFail();
            return;
        }


        if (!codeToValidate.equalsIgnoreCase(codeBuilder.toString())) {
            animateInvalid();
            if (listener != null) listener.onVerificationFail();
            return;
        }


        if (listener != null) listener.onVerificationSuccess();


    }


    public void animateInvalid() {
        if (animation != null && vcAllowAnimation) startAnimation(animation);
    }

    /**
     * Point class
     */

    private static class Point {
        int x;
        int y;
    }

}
