package com.photosynq.app.questions;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.zxing.client.android.CaptureActivity;
import com.photosynq.app.R;
import com.photosynq.app.SelectedOptions;
import com.photosynq.app.model.Question;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import static com.photosynq.app.utils.NxDebugEngine.log;

/**
 * Created by shekhar on 8/19/15.
 * <p/>
 * edited by Manuel Di Cerbo 09 Dec 2015
 */
public class ExpandableListAdapter extends BaseExpandableListAdapter {

    private QuestionsList mQuestionListActivity;
    private final List<Question> mQuestionList;
    public final HashMap<Question, SelectedOptions> mSelectedOptions = new HashMap<>();
    private final ExpandableListView mExpandableListView;
    private final LayoutInflater mLayoutInflater;


    public HashMap<Question, SelectedOptions> getSelectedOptions() {
        return mSelectedOptions;
    }

    public ExpandableListAdapter(QuestionsList questionListActivity, List<Question> questionList, ExpandableListView exp) {
        mQuestionListActivity = questionListActivity;
        mLayoutInflater = (LayoutInflater) questionListActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mExpandableListView = exp;
        mQuestionList = questionList;

        for (Question question : questionList) {
            SelectedOptions selectedOptions = new SelectedOptions();
            selectedOptions.setProjectId(question.getProjectId());
            selectedOptions.setQuestionType(question.getQuestionType());
            selectedOptions.setQuestionId(question.getQuestionId());
            selectedOptions.setSelectedValue(SelectedOptions.TAP_TO_SELECT_ANSWER);
            mSelectedOptions.put(question, selectedOptions);
        }
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        //return this.mQuestionList.get(groupPosition).getOptions().get(childPosititon);
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(final int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, final ViewGroup parent) {
        //final String option = (String) getChild(groupPosition, childPosition);

        final Question question = getGroup(groupPosition);
        if (null != question) {
            switch (question.getQuestionType()) {
                case Question.USER_DEFINED:
                    return inflateUserDefined(question, groupPosition);
                case Question.PROJECT_DEFINED:
                    return inflateProjectDefined(question, groupPosition);
                case Question.PHOTO_TYPE_DEFINED:
                    return inflatePhotoDefined(question, groupPosition);
                default:
                    if (convertView == null) {
                        LayoutInflater infalInflater = (LayoutInflater) this.mQuestionListActivity
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = infalInflater.inflate(R.layout.exp_question_list_item, null);
                    }

                    TextView txtListChildDefault = (TextView) convertView
                            .findViewById(R.id.lblListItem);

                    txtListChildDefault.setText("No options found !");
                    return convertView;

            }

        } else {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.exp_question_list_item, null);
            }

            TextView txtListChildDefault = (TextView) convertView
                    .findViewById(R.id.lblListItem);

            txtListChildDefault.setText("No options found !");
            return convertView;
        }

    }


    @Override
    public int getChildrenCount(int groupPosition) {
//        return mQuestionList.get(groupPosition).getOptions().size();
        return 1;
    }

    @Override
    public Question getGroup(int groupPosition) {
        return mQuestionList.size() > 0 ? mQuestionList.get(groupPosition) : null;
    }

    @Override
    public int getGroupCount() {
        return mQuestionList.size() > 0 ? mQuestionList.size() : 1;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        Question question = getGroup(groupPosition);

        if (question == null) {
            log("question is null, group position %d", groupPosition);
            return mLayoutInflater.inflate(R.layout.list_group_image, null);
        }

        convertView = mLayoutInflater.inflate(R.layout.list_group_image, null);

        ImageView titleThumb = (ImageView) convertView.findViewById(R.id.iv_header_thumb);
        TextView titleView = (TextView) convertView.findViewById(R.id.tv_header_title);
        TextView subtitleView = (TextView) convertView.findViewById(R.id.tv_header_subtitle);

        SelectedOptions so = mSelectedOptions.get(question);

        boolean answerSelected = !so.getSelectedValue().equals(SelectedOptions.TAP_TO_SELECT_ANSWER);

        if (question.getQuestionType() == Question.PHOTO_TYPE_DEFINED) {
            if (answerSelected) {
                String[] splitOptionText = so.getSelectedValue().split(",");
                Picasso.with(mQuestionListActivity)
                        .load(splitOptionText[1])
                        .placeholder(R.drawable.ic_launcher1)
                        .resize(60, 60)
                        .error(R.drawable.ic_launcher1)
                        .into(titleThumb);

                titleView.setText(question.getQuestionText());
                subtitleView.setText("");
            } else {
                titleView.setText(question.getQuestionText());
                subtitleView.setText(SelectedOptions.TAP_TO_SELECT_ANSWER);
            }


            titleThumb.setVisibility(answerSelected ? View.VISIBLE : View.GONE);
        } else {
            titleView.setText(question.getQuestionText());
            subtitleView.setText(so.getSelectedValue());

            titleThumb.setVisibility(View.GONE);
        }

        if (answerSelected) {
            int color = mQuestionListActivity.getResources().getColor(R.color.green_light);
            convertView.setBackgroundColor(color);
        }
        mExpandableListView.setDividerHeight(20);
        checkMeasurementButton();
        return convertView;
    }

    public void selectNextQuestion(){
        for (int i = 0; i < mQuestionList.size(); i++) {
            String val = mSelectedOptions.get(mQuestionList.get(i)).getSelectedValue();
            if(val.equals(SelectedOptions.TAP_TO_SELECT_ANSWER) || val.trim().isEmpty()){
                mExpandableListView.expandGroup(i);
                mExpandableListView.smoothScrollToPositionFromTop(i, 0);
                break;
            }
        }

    }

    private void checkMeasurementButton() {

        boolean allAnswered = true;
        for (SelectedOptions option : mSelectedOptions.values()) {
            if (option.getSelectedValue().equals(SelectedOptions.TAP_TO_SELECT_ANSWER) || option.getSelectedValue().isEmpty()) {
                allAnswered = false;
                break;
            }
        }

        mQuestionListActivity.invalidateMeasurementButton(allAnswered);
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

//    public void setSelectedOptions(ArrayList<SelectedOptions> selectedOptions) {
//        this.mSelectedOptions = selectedOptions;
//    }


    public View inflateProjectDefined(final Question question, final int groupPosition) {
        View convertView = mLayoutInflater.inflate(R.layout.project_defined_option, null);
        CheckBox chkRemember = (CheckBox) convertView.findViewById(R.id.remember_check_box);
        SelectedOptions currentOption = mSelectedOptions.get(question);
        chkRemember.setChecked(currentOption != null && currentOption.isRemember());

        if (currentOption == null) {
            currentOption = new SelectedOptions();
            currentOption.setOptionType(Question.PROJECT_DEFINED);
        }

        chkRemember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedOptions.get(question).setRemember(((CheckBox) v).isChecked());
            }
        });

        LinearLayout currentLayout = null;
        for (int i = 0; i < question.getOptions().size(); i++) {
            TextView tv;
            if (i % 2 == 0) {
                currentLayout = (LinearLayout) mLayoutInflater.inflate(R.layout.user_answer_text_row, null);
                ((LinearLayout) convertView).addView(currentLayout);
                tv = (TextView) currentLayout.findViewById(R.id.view_left);
            } else {
                tv = (TextView) currentLayout.findViewById(R.id.view_right);
            }

            final String option = question.getOptions().get(i);
            tv.setText(option);

            float size = mQuestionListActivity.getResources().getDimensionPixelSize(R.dimen.text_large);

            if (option.length() > 6) {
                size = mQuestionListActivity.getResources().getDimensionPixelSize(R.dimen.text_medium);
            }

            if (option.length() > 20) {
                size = mQuestionListActivity.getResources().getDimensionPixelSize(R.dimen.text_small);
            }

            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
            tv.setTag(new OnClickArgs(question, groupPosition, option));
            tv.setOnClickListener(mOptionSelectedListener);
        }
        return convertView;
    }

    private View inflatePhotoDefined(final Question question, int groupPosition) {

        View convertView = mLayoutInflater.inflate(R.layout.image_options, null);
        SelectedOptions currentOption = mSelectedOptions.get(question);

        CheckBox chkRemember = (CheckBox) convertView.findViewById(R.id.remember_check_box);
        chkRemember.setChecked(currentOption != null && currentOption.isRemember());
        chkRemember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedOptions.get(question).setRemember(((CheckBox) v).isChecked());
            }
        });

        RelativeLayout currentLayout = null;
        for (int i = 0; i < question.getOptions().size(); i++) {
            TextView tv;
            ImageView iv;
            if (i % 2 == 0) {
                currentLayout = (RelativeLayout) mLayoutInflater.inflate(R.layout.user_answer_text_image_row, null);
                ((LinearLayout) convertView).addView(currentLayout);
                tv = (TextView) currentLayout.findViewById(R.id.view_left);
                iv = (ImageView) currentLayout.findViewById(R.id.iv_left);
            } else {
                tv = (TextView) currentLayout.findViewById(R.id.view_right);
                iv = (ImageView) currentLayout.findViewById(R.id.iv_right);
            }

            final String option = question.getOptions().get(i);
            String[] parts = option.split(",");

            if (parts.length != 2) {
                continue;
            }

            tv.setText(parts[0]);
            Picasso.with(mQuestionListActivity)
                    .load(parts[1])
                    .placeholder(R.drawable.ic_launcher1)
                    .error(R.drawable.ic_launcher1)
                    .into(iv);

            OnClickArgs args = new OnClickArgs(question, groupPosition, option);

            iv.setTag(args);
            tv.setTag(args);

            iv.setOnClickListener(mOptionSelectedListener);
            tv.setOnClickListener(mOptionSelectedListener);
        }

        return convertView;


    }

    private View inflateUserDefined(final Question question, int groupPosition) {
        View convertView = mLayoutInflater.inflate(R.layout.user_selected_main_layout, null);
        SelectedOptions currentOption = mSelectedOptions.get(question);

        CheckBox chkRemember = (CheckBox) convertView.findViewById(R.id.remember_check_box);
        chkRemember.setChecked(currentOption != null && currentOption.isRemember());
        chkRemember.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedOptions.get(question).setRemember(((CheckBox) v).isChecked());
            }
        });

        String value = currentOption.getSelectedValue() == null ? "" : currentOption.getSelectedValue();
        Spinner optionType = (Spinner) convertView.findViewById(R.id.option_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mQuestionListActivity, R.array.option_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        optionType.setAdapter(adapter);
        optionType.setTag(new OnClickArgs(question, groupPosition, value));
        optionType.setOnItemSelectedListener(mOptionTypeListener);
        optionType.setSelection(currentOption.getOptionType());

        return convertView;
    }

    private final AdapterView.OnItemSelectedListener mOptionTypeListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            final OnClickArgs args = (OnClickArgs) parent.getTag();

            final Question question = args.mQuestion;
            final int groupPosition = args.mGroupPosition;

            final LinearLayout mainLayout = (LinearLayout) (parent.getParent().getParent());

            LinearLayout userEnteredLayout = (LinearLayout) mainLayout.findViewById(R.id.layout_user_entered);
            LinearLayout scanLayout = (LinearLayout) mainLayout.findViewById(R.id.layout_scan_code);
            LinearLayout autoIncLayout = (LinearLayout) mainLayout.findViewById(R.id.layout_auto_inc);
            CheckBox chkRemember = (CheckBox) mainLayout.findViewById(R.id.remember_check_box);

            final SelectedOptions selectedOption = mSelectedOptions.get(question);

            switch (position) {
                case 0:
                    userEnteredLayout.setVisibility(View.VISIBLE);
                    scanLayout.setVisibility(View.GONE);
                    autoIncLayout.setVisibility(View.GONE);
                    selectedOption.setOptionType(0);
                    EditText txtListChild = (EditText) userEnteredLayout
                            .findViewById(R.id.user_input_edit_text);
                    chkRemember.setVisibility(View.VISIBLE);

                    txtListChild.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                                selectedOption.setSelectedValue(v.getText().toString());
                                checkMeasurementButton();
                                notifyDataSetChanged();
                                return true;
                            } else {
                                return false;
                            }
                        }
                    });

                    if (selectedOption.isReset()) {
                        txtListChild.setText("");
                        selectedOption.setReset(false);
                    } else {
                        if (null != mSelectedOptions.get(question) && !mSelectedOptions.get(question).getSelectedValue().equals(SelectedOptions.TAP_TO_SELECT_ANSWER)) {
                            txtListChild.setText(mSelectedOptions.get(question).getSelectedValue());
                        }
                    }
                    /*
                    if (txtListChild.requestFocus()) {
                        InputMethodManager imm = (InputMethodManager) mQuestionListActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(txtListChild, InputMethodManager.SHOW_IMPLICIT);
                        //((QuestionsList)mQuestionListActivity).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                    */

                    break;
                case 1:
                    userEnteredLayout.setVisibility(View.GONE);
                    scanLayout.setVisibility(View.GONE);
                    autoIncLayout.setVisibility(View.VISIBLE);
                    selectedOption.setOptionType(1);
                    selectedOption.setRemember(true);
                    chkRemember.setVisibility(View.GONE);

                    final EditText fromNumber = (EditText) autoIncLayout
                            .findViewById(R.id.auto_inc_from);
                    final EditText toNumber = (EditText) autoIncLayout
                            .findViewById(R.id.auto_inc_to);
                    final EditText repeatNumber = (EditText) autoIncLayout
                            .findViewById(R.id.auto_inc_repeat);

                    final TextView.OnEditorActionListener listener = new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if ((actionId & EditorInfo.IME_MASK_ACTION) != 0) {
                                if (v.getId() == R.id.auto_inc_from) {
                                    selectedOption.setRangeFrom(v.getText().toString());
                                    toNumber.requestFocus();
                                } else if (v.getId() == R.id.auto_inc_to) {
                                    selectedOption.setRangeTo(v.getText().toString());
                                    repeatNumber.requestFocus();
                                } else if (v.getId() == R.id.auto_inc_repeat) {
                                    selectedOption.setOptionType(1);
                                    selectedOption.setRangeRepeat(v.getText().toString());
                                    selectedOption.setSelectedValue(selectedOption.getRangeFrom() == null ? "" : selectedOption.getRangeFrom());
                                    selectedOption.setAutoIncIndex(0);
                                    notifyDataSetChanged();
                                }
                            }
                            return false; // turns out to be really important as the keyboard stays open returning true
                        }
                    };

                    final View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            TextView tv = (TextView) v;
                            log("has focus %s, tooltip %s", hasFocus+"", tv.getHint());
                            if(!hasFocus){
                                if (v.getId() == R.id.auto_inc_from) {
                                    selectedOption.setRangeFrom(tv.getText().toString());
                                } else if (v.getId() == R.id.auto_inc_to) {
                                    selectedOption.setRangeTo(tv.getText().toString());
                                }
                            }
                        }
                    };

                    fromNumber.setOnEditorActionListener(listener);
                    fromNumber.setOnFocusChangeListener(focusChangeListener);

                    toNumber.setOnEditorActionListener(listener);
                    toNumber.setOnFocusChangeListener(focusChangeListener);

                    repeatNumber.setOnEditorActionListener(listener);
                    repeatNumber.setOnFocusChangeListener(focusChangeListener);


                    //fromNumber.addTextChangedListener(new GenericTextWatcher(fromNumber, question));
                    //toNumber.addTextChangedListener(new GenericTextWatcher(toNumber, question));
                    //repeatNumber.addTextChangedListener(new GenericTextWatcher(repeatNumber, question));

                    if (null != mSelectedOptions.get(question) && !mSelectedOptions.get(question).getSelectedValue().equals("Tap To Select Answer")) {
                        fromNumber.setTag("Wait");
                        toNumber.setTag("Wait");
                        repeatNumber.setTag("Wait");
                        fromNumber.setText(mSelectedOptions.get(question).getRangeFrom());
                        toNumber.setText(mSelectedOptions.get(question).getRangeTo());
                        repeatNumber.setText(mSelectedOptions.get(question).getRangeRepeat());
                        fromNumber.setTag(null);
                        toNumber.setTag(null);
                        repeatNumber.setTag(null);
                    }
                    checkMeasurementButton();
                    break;
                case 2:
                    EditText scannedValue = (EditText) scanLayout
                            .findViewById(R.id.scanned_input_edit_text);
                    Button scanButton = (Button) scanLayout.findViewById(R.id.scan_button);
                    scanButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startCodeScan(groupPosition);
                        }
                    });
                    userEnteredLayout.setVisibility(View.GONE);
                    scanLayout.setVisibility(View.VISIBLE);
                    autoIncLayout.setVisibility(View.GONE);
                    selectedOption.setOptionType(2);
                    chkRemember.setVisibility(View.VISIBLE);


                    scannedValue.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            mSelectedOptions.get(question).setSelectedValue(s.toString());
                            //notifyDataSetChanged();
                        }
                    });

                    if (selectedOption.isReset()) {
                        scannedValue.setText("");
                        selectedOption.setReset(false);
                    } else {
                        if (null != mSelectedOptions.get(question) && !mSelectedOptions.get(question).getSelectedValue().equals(SelectedOptions.TAP_TO_SELECT_ANSWER)) {
                            scannedValue.setText(mSelectedOptions.get(question).getSelectedValue());
                        }
                    }
                    break;
                default:
                    userEnteredLayout.setVisibility(View.VISIBLE);
                    scanLayout.setVisibility(View.GONE);
                    autoIncLayout.setVisibility(View.GONE);
                    selectedOption.setOptionType(0);
                    break;

            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private final View.OnClickListener mOptionSelectedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            OnClickArgs args = (OnClickArgs) v.getTag();
            notifyDataSetChanged();
            mSelectedOptions.get(args.mQuestion).setSelectedValue(args.mOption);
            mExpandableListView.collapseGroup(args.mGroupPosition);
            checkMeasurementButton();
        }
    };

    private static final class OnClickArgs {
        public final Question mQuestion;
        public final int mGroupPosition;
        public final String mOption;

        private OnClickArgs(Question question, int groupPosition, String option) {
            mQuestion = question;
            mGroupPosition = groupPosition;
            mOption = option;
        }
    }

    private void startCodeScan(int groupPosition) {
        Intent intent = new Intent(mQuestionListActivity, CaptureActivity.class);
        intent.setAction("com.google.zxing.client.android.SCAN");
        // this stops saving ur barcode in barcode scanner app's history
        intent.putExtra("SAVE_HISTORY", false);
        ((QuestionsList) mQuestionListActivity).startActivityForResult(intent, groupPosition);
    }

}
