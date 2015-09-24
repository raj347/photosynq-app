package com.photosynq.app.response;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;

import com.photosynq.app.db.DatabaseHelper;
import com.photosynq.app.http.PhotosynqResponse;
import com.photosynq.app.model.Macro;
import com.photosynq.app.model.Option;
import com.photosynq.app.model.Protocol;
import com.photosynq.app.model.Question;
import com.photosynq.app.model.ResearchProject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by shekhar on 9/17/15.
 */

public class MyProjects implements PhotosynqResponse {
    private Context context;
    private SwipeRefreshLayout mListViewContainer;

    public MyProjects(Context context, SwipeRefreshLayout mListViewContainer) {
        this.context = context;
        this.mListViewContainer = mListViewContainer;
    }

    @Override
    public void onResponseReceived(final String result) {

        Thread t = new Thread(new Runnable() {
            public void run() {
                processResult(result);
            }
        });

        t.start();
    }

    private void processResult(String result) {

        Date date = new Date();
        System.out.println("MyProject Start onResponseReceived: " + date.getTime());

        DatabaseHelper db;
        db = DatabaseHelper.getHelper(context);

        JSONArray jArray;

        if (null != result) {

            try {
                JSONObject resultJsonObject = new JSONObject(result);
                if (resultJsonObject.has("projects")) {
                    jArray = resultJsonObject.getJSONArray("projects");

                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject jsonProject = jArray.getJSONObject(i);
                        String protocol_ids = jsonProject.getJSONArray("protocol_ids").toString().trim();

                        JSONObject projectImageUrl = jsonProject.getJSONObject("project_photo");//get project image url.
                        JSONObject creatorJsonObj = jsonProject.getJSONObject("creator");//get project creator infos.
                        JSONObject creatorAvatar = creatorJsonObj.getJSONObject("avatar");//
                        JSONArray protocols = jsonProject.getJSONArray("protocols");

                        ResearchProject rp = new ResearchProject(
                                jsonProject.getString("id"),
                                jsonProject.getString("name"),
                                jsonProject.getString("description"),
                                jsonProject.getString("directions_to_collaborators"),
                                creatorJsonObj.getString("id"),
                                jsonProject.getString("start_date"),
                                jsonProject.getString("end_date"),
                                projectImageUrl.getString("original"),
                                jsonProject.getString("beta"),
                                jsonProject.getString("is_contributed"),
                                protocol_ids.substring(1, protocol_ids.length() - 1),
                                creatorJsonObj.getString("name"),
                                creatorJsonObj.getString("contributions"),
                                creatorAvatar.getString("thumb")); // remove first and last square bracket and store as a comma separated string

                        db.deleteOptions(rp.id);

                        db.deleteQuestions(rp.id);

                        JSONArray customFields = jsonProject.getJSONArray("filters");
                        for (int j = 0; j < customFields.length(); j++) {
                            JSONObject jsonQuestion = customFields.getJSONObject(j);
                            int questionType = Integer.parseInt(jsonQuestion.getString("value_type"));
                            JSONArray optionValuesJArray = jsonQuestion.getJSONArray("value");
                            //Sometime option value is empty i.e we need to set "" parameter.
                            if (optionValuesJArray.length() == 0) {
                                Option option = new Option(jsonQuestion.getString("id"), "", jsonProject.getString("id"));
                                db.updateOption(option);
                            }
                            for (int k = 0; k < optionValuesJArray.length(); k++) {
                                if (Question.PROJECT_DEFINED == questionType) { //If question type is project_defined then save options.
                                    String getSingleOption = optionValuesJArray.getString(k);
                                    Option option = new Option(jsonQuestion.getString("id"), getSingleOption, jsonProject.getString("id"));
                                    db.updateOption(option);
                                } else if (Question.PHOTO_TYPE_DEFINED == questionType) { //If question type is photo_type then save options and option image.
                                    JSONObject options = optionValuesJArray.getJSONObject(k);
                                    String optionString = options.getString("answer");
                                    String optionImage = options.getString("medium");//get option image if question type is Photo_Type
                                    Option option = new Option(jsonQuestion.getString("id"), optionString + "," + optionImage, jsonProject.getString("id"));
                                    db.updateOption(option);
                                }
                            }

                            Question question = new Question(
                                    jsonQuestion.getString("id"),
                                    jsonProject.getString("id"),
                                    jsonQuestion.getString("label"),
                                    questionType);
                            db.updateQuestion(question);

                            for (int proto = 0; proto < protocols.length(); proto++) {

                                JSONObject protocolobj = protocols.getJSONObject(proto);
                                String id = protocolobj.getString("id");
                                System.out.println("Protocol ID " + id);
                                Protocol protocol = new Protocol(id,
                                        protocolobj.getString("name"),
                                        protocolobj.getString("protocol_json"),
                                        protocolobj.getString("description"),
                                        protocolobj.getString("macro_id"), "slug",
                                        protocolobj.getString("pre_selected"));
                                JSONObject macroobject = protocolobj.getJSONObject("macro");
                                Macro macro = new Macro(macroobject.getString("id"),
                                        macroobject.getString("name"),
                                        macroobject.getString("description"),
                                        macroobject.getString("default_x_axis"),
                                        macroobject.getString("default_y_axis"),
                                        macroobject.getString("javascript_code"),
                                        "slug");
                                System.out.println("Macro ID "+macro.getId());
                                db.updateMacro(macro);

                                db.updateProtocol(protocol);
                            }
                        }
                        db.updateResearchProject(rp);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mListViewContainer.setRefreshing(false);
        Date date1 = new Date();
        System.out.println("MyProject End onResponseReceived: " + date1.getTime());
    }
}
