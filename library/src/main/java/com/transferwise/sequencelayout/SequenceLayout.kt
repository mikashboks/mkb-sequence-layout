package com.transferwise.sequencelayout

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.StyleRes
import androidx.core.view.ViewCompat
import androidx.core.view.children
import com.mikashboks.sequencelayout.R

/**
 * Vertical step tracker that contains {@link com.transferwise.sequencelayout.SequenceStep}s and animates to the first active step.
 *
 * <pre>
 * &lt;com.transferwise.sequencelayout.SequenceLayout
 *      android:layout_width="match_parent"
 *      android:layout_height="wrap_content"
 *      app:stepVerticalSpace="4dp"
 *      app:progressForegroundColor="?colorAccent"
 *      app:progressBackgroundColor="#ddd"&gt;
 *
 *      &lt;com.transferwise.sequencelayout.SequenceStep ... /&gt;
 *      &lt;com.transferwise.sequencelayout.SequenceStep app:active="true" ... /&gt;
 *      &lt;com.transferwise.sequencelayout.SequenceStep ... /&gt;
 *
 * &lt;/com.transferwise.sequencelayout.SequenceLayout&gt;
 * </pre>
 *
 * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_stepVerticalSpace
 * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_progressForegroundColor
 * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_progressBackgroundColor
 *
 * @see com.transferwise.sequencelayout.SequenceStep
 */
public class SequenceLayout(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    FrameLayout(context, attrs, defStyleAttr), SequenceStep.OnStepChangedListener {

    public constructor(context: Context) : this(context, null)
    public constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.sequence_layout, this, true)
    }

    private val progressBarForeground = findViewById<View>(R.id.progressBarForeground)
    private val progressBarBackground = findViewById<View>(R.id.progressBarBackground)
    private val progressBarWrapper = findViewById<View>(R.id.progressBarWrapper)
    private val stepsWrapper = findViewById<ViewGroup>(R.id.stepsWrapper)
    private val dotsWrapper = findViewById<ViewGroup>(R.id.dotsWrapper)

    init {
        val attributes = getContext().theme.obtainStyledAttributes(
            attrs, R.styleable.SequenceLayout, 0, R.style.SequenceLayout
        )
        applyAttributes(attributes)
        attributes.recycle()

        clipToPadding = false
        clipChildren = false
    }

    @ColorInt
    private var progressBackgroundColor: Int = 0

    @ColorInt
    private var progressForegroundColor: Int = 0

    @DimenRes
    private var stepVerticalSpace: Int = 0

    private var progressDotStepCurrent: Int = 0
    private var progressDotStepMax: Int = 0

    private var firstOffset = 0
    private var lastOffset = 0
    private var progressStepOffSet = 0


    public fun setStyle(@StyleRes defStyleAttr: Int) {
        val attributes =
            context.theme.obtainStyledAttributes(defStyleAttr, R.styleable.SequenceLayout)
        applyAttributes(attributes)
        attributes.recycle()
    }

    /**
     * Sets the step progress current value
     *
     * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_progressDotStepCurrent
     */
    public fun setProgressDotStepCurrent(progressDotStepCurrent: Int) {
        this.progressDotStepCurrent = progressDotStepCurrent
        progressToNextStep()
    }

    /**
     * Sets the step progress max value
     *
     * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_progressDotStepMax
     */
    public fun setProgressDotStepMax(progressDotStepMax: Int) {
        this.progressDotStepMax = progressDotStepMax
    }

    /**
     * Sets the step vertical space
     *
     * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_stepVerticalSpace
     */
    public fun setStepVerticalSpace(@DimenRes stepVerticalSpace: Int) {
        this.stepVerticalSpace = stepVerticalSpace
        requestLayout()
    }


    /**
     * Sets the progress bar color
     *
     * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_progressForegroundColor
     */
    public fun setProgressForegroundColor(@ColorInt color: Int) {
        this.progressForegroundColor = color
        progressBarForeground.setBackgroundColor(color)
        //TODO apply to existing steps
    }

    /**
     * Sets background resource for the dot of each contained step
     *
     * @attr ref com.transferwise.sequencelayout.R.styleable#SequenceLayout_dotBackground
     */
    public fun setProgressBackgroundColor(@ColorInt progressBackgroundColor: Int) {
        this.progressBackgroundColor = progressBackgroundColor
        progressBarBackground.setBackgroundColor(progressBackgroundColor)
        //TODO apply to existing steps
    }

    /**
     * Removes all contained [com.transferwise.sequencelayout.SequenceStep]s
     */
    public fun removeAllSteps() {
        stepsWrapper.removeAllViews()
    }

    /**
     * Replaces all contained [com.transferwise.sequencelayout.SequenceStep]s with those provided and bound by the adapter
     */
    public fun <T> setAdapter(adapter: SequenceAdapter<T>) where T : Any {
        removeCallbacks(animateToActive)
        removeAllSteps()
        val count = adapter.getCount()
        for (i in 0 until count) {
            val item = adapter.getItem(i)
            val view = SequenceStep(context)
            adapter.bindView(view, item)
            addView(view)
        }
    }

    private fun applyAttributes(attributes: TypedArray) {
        setupProgressForegroundColor(attributes)
        setupProgressBackgroundColor(attributes)
        setupStepVerticalSpace(attributes)
    }

    private fun setupStepVerticalSpace(attributes: TypedArray) {
        setStepVerticalSpace(
            attributes.getDimensionPixelSize(R.styleable.SequenceLayout_stepVerticalSpace, 0)
        )
    }

    private fun setupProgressForegroundColor(attributes: TypedArray) {
        setProgressForegroundColor(
            attributes.getColor(
                R.styleable.SequenceLayout_progressForegroundColor, 0
            )
        )
    }

    private fun setupProgressBackgroundColor(attributes: TypedArray) {
        setProgressBackgroundColor(
            attributes.getColor(
                R.styleable.SequenceLayout_progressBackgroundColor, 0
            )
        )
    }

    private fun setProgressBarHorizontalOffset() {
        val firstAnchor: View = stepsWrapper.getChildAt(0).findViewById(R.id.anchor)
        progressBarWrapper.translationX =
            firstAnchor.measuredWidth + 4.toPx() - (progressBarWrapper.measuredWidth / 2f) //TODO dynamic dot size
    }

    private fun placeDots() {
        dotsWrapper.removeAllViews()
        firstOffset = 0
        lastOffset = 0

        stepsWrapper.children().forEachIndexed { i, view ->
            val sequenceStep = view as SequenceStep
            val sequenceStepDot = SequenceStepDot(context)
            sequenceStepDot.setDotBackground(progressForegroundColor, progressBackgroundColor)
            sequenceStepDot.setPulseColor(progressForegroundColor)
            sequenceStepDot.clipChildren = false
            sequenceStepDot.clipToPadding = false
            val layoutParams = LayoutParams(8.toPx(), 8.toPx()) //TODO dynamic dot size
            val totalDotOffset = getRelativeTop(
                sequenceStep, stepsWrapper
            ) + sequenceStep.paddingTop + sequenceStep.getDotOffset() + 2.toPx() //TODO dynamic dot size
            layoutParams.topMargin = totalDotOffset
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
            dotsWrapper.addView(sequenceStepDot, layoutParams)
            if (i == 0) {
                firstOffset = totalDotOffset
            }
            lastOffset = totalDotOffset
        }

        val backgroundLayoutParams = progressBarBackground.layoutParams as MarginLayoutParams
        backgroundLayoutParams.topMargin = firstOffset + 4.toPx() //TODO dynamic dot size
        backgroundLayoutParams.height = lastOffset - firstOffset
        progressBarBackground.requestLayout()

        val foregroundLayoutParams = progressBarForeground.layoutParams as MarginLayoutParams
        foregroundLayoutParams.topMargin = firstOffset + 4.toPx() //TODO dynamic dot size
        foregroundLayoutParams.height = lastOffset - firstOffset
        progressBarForeground.requestLayout()

        progressToNextStep()
    }

    private fun progressToNextStep() {
        progressStepOffSet = 0

        val activeStepIndex = stepsWrapper.children().indexOfLast { it is SequenceStep && it.isActive() }

        // Validation for show progress to next step
        if (activeStepIndex >= (stepsWrapper.children().count() - 1) ||
            progressDotStepCurrent > progressDotStepMax || activeStepIndex == -1
        ) {
            progressStepOffSet = 0
            return
        }

        var prevStepOffSet = 0
        stepsWrapper.children()
            .forEachIndexed { i, view ->

                if (i == activeStepIndex) {
                    val totalDotOffset = getRelativeTop(view)
                    prevStepOffSet = totalDotOffset
                }

                if (i == (activeStepIndex + 1)) {
                    val totalDotOffset = getRelativeTop(view)
                    progressStepOffSet = totalDotOffset - prevStepOffSet
                    return@forEachIndexed
                }
            }

        progressStepOffSet = (progressDotStepCurrent.toFloat() / progressDotStepMax.toFloat() * progressStepOffSet.toFloat()).toInt()
        post(animateToActive)
    }

    private fun getRelativeTop(view: View?): Int {
        val sequenceStep = view as SequenceStep
        return getRelativeTop(sequenceStep, stepsWrapper) +
                sequenceStep.paddingTop +
                sequenceStep.getDotOffset() + 2.toPx()
    }

    private val animateToActive = {
        progressBarForeground.visibility = VISIBLE
        progressBarForeground.pivotY = 0f
        progressBarForeground.scaleY = 0f

        val activeStepIndex =
            stepsWrapper.children().indexOfLast { it is SequenceStep && it.isActive() }

        if (activeStepIndex != -1) {
            val activeDot = dotsWrapper.getChildAt(activeStepIndex)
            if (activeDot != null) {
                val activeDotTopMargin = (activeDot.layoutParams as LayoutParams).topMargin
                val progressBarForegroundTopMargin =
                    (progressBarForeground.layoutParams as LayoutParams).topMargin
                val scaleEnd =
                    (activeDotTopMargin + progressStepOffSet + (activeDot.measuredHeight / 2) -
                            progressBarForegroundTopMargin) / progressBarBackground.measuredHeight.toFloat()

                ViewCompat.animate(progressBarForeground)
                    .setStartDelay(resources.getInteger(R.integer.sequence_step_duration).toLong())
                    .scaleY(scaleEnd).setInterpolator(LinearInterpolator()).setDuration(
                        activeStepIndex * resources.getInteger(R.integer.sequence_step_duration)
                            .toLong()
                    ).setUpdateListener {
                        val animatedOffset =
                            progressBarForeground.scaleY * progressBarBackground.measuredHeight
                        dotsWrapper.children().forEachIndexed { i, view ->
                            if (i > activeStepIndex) {
                                return@forEachIndexed
                            }
                            val dot = view as SequenceStepDot
                            val dotTopMargin =
                                (dot.layoutParams as LayoutParams).topMargin - progressBarForegroundTopMargin - (dot.measuredHeight / 2)
                            if (animatedOffset >= dotTopMargin) {
                                if (i < activeStepIndex && !dot.isEnabled) {
                                    dot.isEnabled = true
                                } else if (i == activeStepIndex && !dot.isActivated) {
                                    dot.isActivated = true
                                }
                            }
                        }
                    }.withEndAction {

                        postDelayed({
                            // Animate Every activated dot
                            stepsWrapper.children.forEachIndexed { index, view ->
                                if (view is SequenceStep && view.isActive()) {
                                    dotsWrapper.children().getOrNull(index)?.apply {
                                        if (this is SequenceStepDot) {
                                            this.isEnabled = true
                                            this.isActivated = true
                                        }
                                    }
                                }
                            }
                        }, 500)
                    }.start()
            }
        }
    }

    private fun getRelativeTop(child: View, parent: ViewGroup): Int {
        val offsetViewBounds = Rect()
        child.getDrawingRect(offsetViewBounds)
        parent.offsetDescendantRectToMyCoords(child, offsetViewBounds)
        return offsetViewBounds.top
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child is SequenceStep) {
            child.onStepChangedListener = this

            if (stepsWrapper.children().isNotEmpty()) {
                child.setPadding(0, stepVerticalSpace, 0, 0)
            }

            stepsWrapper.addView(child, params)
            return
        }
        super.addView(child, index, params)
    }

    override fun onStepChanged() {
        setProgressBarHorizontalOffset()
        placeDots()
        removeCallbacks(animateToActive)
        post(animateToActive)
    }
}