import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by liufeng
 * 在线填空题
 */

public class FillBlankView {
    private List<String> answers;
    private int editTextSize = 16;
    private TextView textView;
    private List<EditText> editTexts = new ArrayList<>();
    private String splitReg = "\\_{5,}";

    public FillBlankViewListener fillBlankViewListener;

    public FillBlankView() {}
    public FillBlankView(TextView textView) {
        this.textView = textView;
    }

    public interface FillBlankViewListener {
        public void onDraw();
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
        if (textView == null) {
            return;
        }
        int i = 0;
        String string = textView.getText().toString();
        Pattern pattern = Pattern.compile(splitReg);
        while(pattern.matcher(string).find()){
            if (answers.size() <= i) break;
            String answer = answers.get(i);
            answer = answer.replaceAll(" ", "'");//将空格替换为',防止词组分行显示,导致排版问题
            string = string.replaceFirst(splitReg, String.format("[[%s]]", answer));
            i++;
        }
        textView.setText(string);
    }

    public void setEditTextSize(int editTextSize) {
        this.editTextSize = editTextSize;
        for(EditText editText : editTexts) {
            editText.setTextSize(editTextSize);
        }
    }

    public void enabledEditTexts() {
        for (EditText editText : editTexts) {
            editText.setEnabled(true);
            editText.setBackgroundResource(R.drawable.onfocusno);
        }
    }

    /**
     * 重置输入框
     */
    public void reset() {
        for (EditText editText : editTexts) {
            editText.setText("");
            editText.setEnabled(true);
//            editText.setTextColor(textView.getResources().getColor(android.R.color.holo_blue_bright));
        }
    }

    /**
     * 设置要替换的目标TextView及文本
     * @param context
     * @param targetTextView
     * @return
     */
    public View setTargetViewAndText(final Context context, final TextView targetTextView) {
//        String text = targetTextView.getText().toString();
//        if (text == null || text.indexOf(targetText) < 0) {
//            return null;
//        }
        textView = targetTextView;
        final FrameLayout container = decorateTextView(context, targetTextView);
        targetTextView.post(new Runnable() {
            @Override
            public void run() {
                List<TextDemensions> demensions = replaceTargetTextAndGetDemensions(targetTextView);
                int lineHight= (int) (targetTextView.getMeasuredHeight() * 1.0F / targetTextView.getLineCount());
                generateEditText(context,container,demensions,lineHight);
                if (fillBlankViewListener != null) {
                    fillBlankViewListener.onDraw();
                }
            }
        });
        return container;
    }

    /**
     * 根据获得字符的位置数据生成EditText进行替换
     * @param context
     * @param container
     * @param demensions
     * @param height
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void generateEditText(Context context, FrameLayout container, List<TextDemensions> demensions, int height) {
        for(TextDemensions dem:demensions){
            final EditText editText=new EditText(context);
            editText.setTextSize(editTextSize);
            editText.setBackgroundResource(R.drawable.onfocusno);
            editText.setSingleLine();
            editText.setPadding(10,0,10,0);
            editText.setGravity(Gravity.CENTER);
//            editText.setHint("");
//            editText.setTextColor(container.getResources().getColor(android.R.color.holo_blue_bright));

            final float scale = container.getResources().getDisplayMetrics().density;
            height = (int) (28 * scale + 0.5f);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) dem.mTextWidth,height);
            container.addView(editText,lp);
            editText.setTranslationX(dem.mStartX);
            editText.setTranslationY(dem.mStartY);
//            editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//                @Override
//                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                    if (KeyEvent.KEYCODE_ENTER == event.getKeyCode()) {
//                        int index = editTexts.indexOf(v);
//                        index++;
//                        if (index < editTexts.size() - 1) {
//                            editTexts.get(index).requestFocus();
//                            return false;
//                        }
//                    }
//                    return true;
//                }
//            });
            editTexts.add(editText);
        }
    }

    /**
     * 将TextView用FrameLayout进行包装
     * @param context
     * @param targetTextView
     * @return
     */
    private FrameLayout decorateTextView(Context context, TextView targetTextView) {
        int index = 0;
        FrameLayout container = new FrameLayout(context);
        ViewGroup parent = (ViewGroup) targetTextView.getParent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i) == targetTextView) {
                index = i;
            }
        }
        ViewGroup.LayoutParams lp = targetTextView.getLayoutParams();
        parent.removeView(targetTextView);
        container.addView(targetTextView);
        container.setLayoutParams(lp);
        parent.addView(container, index);
        return container;
    }

    /**
     * 将需要填空的字符串使用SpannableStirng 进行隐藏,同时生成其位置数据及尺寸信息
     * @param targetTextView
     * @return
     */
    private List<TextDemensions> replaceTargetTextAndGetDemensions(TextView targetTextView) {
        editTexts.clear();

        List<TextDemensions> textDimensions = null;
        textDimensions = new ArrayList<>();
        TextPaint textPaint = targetTextView.getPaint();
        String text = targetTextView.getText().toString();
        int pos = -1;
        SpannableString sp = new SpannableString(text);

        int lineHeight=targetTextView.getHeight() / targetTextView.getLineCount();
        Layout layout=targetTextView.getLayout();//layout是用于进行展示Text

        List<Integer> lineWidth=new ArrayList<>(targetTextView.getLineCount());
        for(int i=0;i<layout.getLineCount();i++){
            lineWidth.add((int) layout.getLineWidth(i));
        }
        int j = 0;
        String targetText;
        do {
            if (answers.size() == j) break;
            String answer = answers.get(j);
            answer = answer.replaceAll(" ", "'");//将空格替换为',防止词组分行显示,导致排版问题
            targetText = "[["+answer+"]]";
            pos = text.indexOf(targetText, pos + 1);
            if (pos < 0) {
                break;
            }
            int length = (int) textPaint.measureText(text, 0, pos);

            sp.setSpan(new ForegroundColorSpan(targetTextView.getDrawingCacheBackgroundColor()), pos, targetText.length() + pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextDemensions dim = new TextDemensions();
            int i;
            for(i=0;i<lineWidth.size();i++){
                if(length<lineWidth.get(i)){
                    break;
                }
                length-=lineWidth.get(i);
            }
            dim.mStartY=i*lineHeight;
            dim.mStartX = length;
            dim.mTextWidth = textPaint.measureText(targetText);
            textDimensions.add(dim);
            j++;
        } while (true);
        targetTextView.setText(sp);
        return textDimensions;
    }

    /**
     * 提交用户答案
     * @return 返回用户答案
     */
    public ArrayList<String> submit() {
        ArrayList<String> userAnswers = new ArrayList<>();
        for (int i = 0; i < editTexts.size(); i++) {
            EditText editText = editTexts.get(i);
            userAnswers.add(editText.getText().toString());
        }
        setUserAnswers(userAnswers);
        return userAnswers;
    }

    /**
     * 设置用户答案
     * @param userAnswers 用户大题记录
     */
    public void setUserAnswers(List<String> userAnswers) {
        if (userAnswers == null) return;
        for (int i = 0; i < editTexts.size(); i++) {
            String userAnswer = "";
            EditText editText = editTexts.get(i);
            if (userAnswers.size() - 1 >= i) {
                userAnswer = userAnswers.get(i);
                editText.setText(userAnswer);
            }
            if (checkFillAnswer(answers.get(i), userAnswer)){// userAnswer.equalsIgnoreCase(answers.get(i))) {
//                editText.setTextColor(Color.GREEN);
                editText.setBackgroundResource(R.drawable.edittext_answer_right_bg);
            }else {
//                editText.setTextColor(Color.RED);
                editText.setBackgroundResource(R.drawable.edittext_answer_wrong_bg);
            }
            editText.setEnabled(false);
            if (editText.hasFocus()) {
                editText.clearFocus();
            }
        }
    }

    private boolean checkFillAnswer(String answer, String myAnswer) {
        if (answer != null) {
            myAnswer = myAnswer.replaceAll(Constant.RegString.FILL_REG, "");
            String[] answers = answer.split("\\|");
            for (int i = 0; i < answers.length; i++) {
                String _answer = answers[i].trim();
                _answer = _answer.replaceAll(Constant.RegString.FILL_REG, "");
                if (_answer.equalsIgnoreCase(myAnswer.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 表示文本的尺寸信息的数据结构
     */
    private class TextDemensions {
        public float mStartX;
        public float mStartY;
        public float mTextWidth;
    }
    
    public static void setLineHeight(TextView textView, int lineHeight) {
        int fontHeight = textView.getPaint().getFontMetricsInt(null);
        textView.setLineSpacing(dpToPixel(textView.getContext(), lineHeight) - fontHeight, 1);
    }

    public static int dpToPixel(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }
}
