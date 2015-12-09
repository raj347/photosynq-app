package com.photosynq.app.questions;

import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
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

import com.google.android.gms.games.quest.Quest;
import com.google.zxing.client.android.CaptureActivity;
import com.photosynq.app.R;
import com.photosynq.app.SelectedOptions;
import com.photosynq.app.model.Option;
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

    private Context mContext;
    private final List<Question> mQuestionList;
    public final HashMap<Question, SelectedOptions> mSelectedOptions = new HashMap<>();
    private final ExpandableListView mExpandableListView;
    private final LayoutInflater mLayoutInflater;


    public HashMap<Question, SelectedOptions> getSelectedOptions() {
        return mSelectedOptions;
    }

    public ExpandableListAdapter(Context context, List<Question> questionList, ExpandableListView exp) {
        mContext = context;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
                        LayoutInflater infalInflater = (LayoutInflater) this.mContext
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
                Picasso.with(mContext)
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
            int color = mContext.getResources().getColor(R.color.green_light);
            convertView.setBackgroundColor(color);
        }
        mExpandableListView.setDividerHeight(20);
        checkMeasurementButton();
        return convertView;
    }

    private void checkMeasurementButton() {

        ViewParent v = mExpandableListView.getParent().getParent();
        Button btnTakeMeasurement = (Button) ((RelativeLayout) v).findViewById(R.id.btn_take_measurement);
        boolean flag = false;
        for (SelectedOptions option : mSelectedOptions.values()) {
            if (option.getSelectedValue().equals(SelectedOptions.TAP_TO_SELECT_ANSWER) || option.getSelectedValue().isEmpty()) {
                flag = true;
                break;
            }
        }

        if (flag) {
            btnTakeMeasurement.setText("Answer All Questions");
            btnTakeMeasurement.setBackgroundResource(R.drawable.btn_layout_gray_light);
        } else {
            if (!btnTakeMeasurement.getText().equals("Cancel")) {
                btnTakeMeasurement.setText("+ Take Measurement");
                btnTakeMeasurement.setBackgroundResource(R.drawable.btn_layout_orange);
            }

        }

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

            float size = mContext.getResources().getDimensionPixelSize(R.dimen.text_large);

            if (option.length() > 6) {
                size = mContext.getResources().getDimensionPixelSize(R.dimen.text_medium);
            }

            if (option.length() > 20) {
                size = mContext.getResources().getDimensionPixelSize(R.dimen.text_small);
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
            Picasso.with(mContext)
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
        optionType.setTag(new OnClickArgs(question, groupPosition, value));
        optionType.setOnItemSelectedListener(mOptionTypeListener);

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

                    txtListChild.addTextChangedListener(new TextWatcher() {

                        private String mCache = "";
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            selectedOption.setSelectedValue(s.toString());
                            checkMeasurementButton();

                            // TODO adapt parent view manually without notifydatasetchanged, or trigger at end when user enters the data
                            if(!mCache.equals(s.toString())){
                                mCache = s.toString();
                            }
                            //notifyDataSetChanged();
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
                        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(txtListChild, InputMethodManager.SHOW_IMPLICIT);
                        //((QuestionsList)mContext).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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

                    EditText fromNumber = (EditText) autoIncLayout
                            .findViewById(R.id.auto_inc_from);
                    EditText toNumber = (EditText) autoIncLayout
                            .findViewById(R.id.auto_inc_to);
                    EditText repeatNumber = (EditText) autoIncLayout
                            .findViewById(R.id.auto_inc_repeat);

                    fromNumber.addTextChangedListener(new GenericTextWatcher(fromNumber, question));
                    toNumber.addTextChangedListener(new GenericTextWatcher(toNumber, question));
                    repeatNumber.addTextChangedListener(new GenericTextWatcher(repeatNumber, question));

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
                            Intent intent = new Intent(mContext, CaptureActivity.class);
                            intent.setAction("com.google.zxing.client.android.SCAN");
                            // this stops saving ur barcode in barcode scanner app's history
                            intent.putExtra("SAVE_HISTORY", false);
                            ((QuestionsList) mContext).startActivityForResult(intent, groupPosition);
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

    private class GenericTextWatcher implements TextWatcher {

        private final View mView;
        private final Question mQuestion;

        private GenericTextWatcher(View view, Question question) {
            this.mView = view;
            this.mQuestion = question;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            ExpandableListView exp = (ExpandableListView) (mView.getParent().getParent().getParent().getParent());
            View headerView = exp.findViewWithTag(mQuestion);
            String s = editable.toString();
            SelectedOptions selectedOption = mSelectedOptions.get(mQuestion);
            if (!s.isEmpty()) {

                switch (mView.getId()) {
                    case R.id.auto_inc_from:
                        selectedOption.setRangeFrom(s);
                        break;
                    case R.id.auto_inc_to:
                        selectedOption.setRangeTo(s);
                        break;
                    case R.id.auto_inc_repeat:
                        selectedOption.setRangeRepeat(s);
                        break;
                }
            } else {
                switch (mView.getId()) {
                    case R.id.auto_inc_from:
                        selectedOption.setRangeFrom("");
                        break;
                    case R.id.auto_inc_to:
                        selectedOption.setRangeTo("");
                        break;
                    case R.id.auto_inc_repeat:
                        selectedOption.setRangeRepeat("");
                        break;

                }

            }
            TextView selectedAnswer = (TextView) headerView.findViewById(R.id.tv_header_subtitle);
            if (!selectedOption.getRangeFrom().isEmpty() && !selectedOption.getRangeTo().isEmpty() && !selectedOption.getRangeRepeat().isEmpty()) {
                if (mView.getTag() == null) {
                    selectedOption.setSelectedValue(selectedOption.getRangeFrom());
                    selectedAnswer.setText(selectedOption.getRangeFrom());
                    selectedOption.setAutoIncIndex(0);
                }
            } else {
                selectedOption.setSelectedValue("");
                selectedAnswer.setText("");

            }

            checkMeasurementButton();
            //notifyDataSetChanged();

        }
    }


    /* old handler for createView */

    /**
     NoDefaultSpinner projectDefinedOptionsSpinner = (NoDefaultSpinner) convertView
     .findViewById(R.id.project_defined_options_spinner);

     List<String> list = getGroup(groupPosition).getOptions();
     ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(convertView.getContext(),
     R.layout.simple_spinner_item, list);

     dataAdapter.setDropDownViewResource(R.layout.spinner_text);
     projectDefinedOptionsSpinner.setAdapter(dataAdapter);
     projectDefinedOptionsSpinner.setTag(groupPosition + "-" + childPosition);
     projectDefinedOptionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    @Override public void onItemSelected(AdapterView<?> parent, View mView, int position, long id) {
    String[] ids = ((String) parent.getTag()).split("-");
    int questionNumber = Integer.parseInt(ids[0]);
    //int questionNumber = (int)parent.getTag();
    SelectedOptions so = mSelectedOptions.get(questionNumber);
    so.setSelectedValue(parent.getItemAtPosition(position).toString());
    mSelectedOptions.set(questionNumber, so);

    LinearLayout ll = (LinearLayout) parent.getParent();
    ExpandableListView explist = (ExpandableListView) ll.getParent();

    LinearLayout ll2 = (LinearLayout) explist.findViewWithTag(questionNumber);
    if (null != ll2) {
    TextView selectedAnswer = (TextView) ll2.findViewById(R.id.selectedAnswer);
    selectedAnswer.setText(parent.getItemAtPosition(position).toString());

    final int sdk = android.os.Build.VERSION.SDK_INT;
    if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
    ll2.setBackgroundDrawable(mContext.getResources().getDrawable(R.color.green_light));
    } else {
    ll2.setBackground(mContext.getResources().getDrawable(R.color.green_light));
    }
    }
    checkMeasurementButton();
    if (collapse[0]) {
    mExpandableListView.collapseGroup(groupPosition);
    } else {
    collapse[0] = true;
    }

    }

    @Override public void onNothingSelected(AdapterView<?> parent) {

    }
    });

     if (so1.isReset()) {
     projectDefinedOptionsSpinner.setSelection(-1);
     so1.setReset(false);
     mSelectedOptions.set(groupPosition, so1);

     } else {
     projectDefinedOptionsSpinner.setSelection(dataAdapter.getPosition(mSelectedOptions.get(groupPosition).getSelectedValue()));
     collapse[0] = false;
     }
     **/


    /* old image option code */

/**

        NoDefaultSpinner photoDefinedOptionsSpinner = (NoDefaultSpinner) convertView
                .findViewById(R.id.image_options_spinner);
        List<String> list1 = getGroup(groupPosition).getOptions();
        ImageSpinnerAdapter dataAdapter1 = new ImageSpinnerAdapter(convertView.getContext(),
                R.layout.spinner_image_text, list1);

        dataAdapter1.setDropDownViewResource(R.layout.spinner_image_text);

        final SelectedOptions selectedOption = mSelectedOptions.get(question);

        photoDefinedOptionsSpinner.setAdapter(dataAdapter1);
        photoDefinedOptionsSpinner.setTag(groupPosition + "-" + childPosition);
        photoDefinedOptionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] ids = ((String) parent.getTag()).split("-");
                int questionNumber = Integer.parseInt(ids[0]);
                selectedOption.setSelectedValue(parent.getItemAtPosition(position).toString());

//                            mSelectedOptions.set(questionNumber, parent.getItemAtPosition(position).toString());
                LinearLayout ll = (LinearLayout) parent.getParent();
                ExpandableListView explist = (ExpandableListView) ll.getParent();

                LinearLayout ll2 = (LinearLayout) explist.findViewWithTag(questionNumber);
                if (null != ll2) {
                    ImageView lblListHeader_image = (ImageView) ll2.findViewById(R.id.iv_header_thumb);
                    TextView selectedAnswer = (TextView) ll2.findViewById(R.id.tv_header_subtitle);
                    selectedAnswer.setText("");

                    String[] splitOptionText = mSelectedOptions.get(question).getSelectedValue().toString().split(",");
                    Picasso.with(mContext)
                            .load(splitOptionText[1])
                            .placeholder(R.drawable.ic_launcher1)
                            .resize(60, 60)
                            .error(R.drawable.ic_launcher1)
                            .into(lblListHeader_image);

                    final int sdk = android.os.Build.VERSION.SDK_INT;
                    if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                        ll2.setBackgroundDrawable(mContext.getResources().getDrawable(R.color.green_light));
                    } else {
                        ll2.setBackground(mContext.getResources().getDrawable(R.color.green_light));
                    }
                }
                checkMeasurementButton();
                if (collapse1[0]) {
                    mExpandableListView.collapseGroup(groupPosition);
                } else {
                    collapse1[0] = true;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if (so2.isReset()) {
            photoDefinedOptionsSpinner.setSelection(-1);
            so2.setReset(false);
        } else {
            photoDefinedOptionsSpinner.setSelection(dataAdapter1.getPosition(mSelectedOptions.get(question).getSelectedValue()));
            collapse1[0] = false;
        }


//                    TextView txtListChild3 = (TextView) convertView
//                            .findViewById(R.id.lblListItem);
//
//                    txtListChild3.setText("Please handle me 3");
        return convertView;

           **/


}
