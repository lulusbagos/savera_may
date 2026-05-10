package id.icapps.savera.activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.util.GB;

public class MyP5M extends Fragment {
    private final AtomicBoolean requestRunning = new AtomicBoolean(false);
    private final Map<Integer, RadioGroup> answerGroups = new LinkedHashMap<>();
    private final Map<Integer, String> questionByItemId = new LinkedHashMap<>();

    private ProgressBar progress;
    private NestedScrollView p5mScroll;
    private LinearLayout statusCard;
    private TextView textStatusBadge;
    private TextView textStatus;
    private TextView textStatusSubtitle;
    private TextView textQuizSource;
    private TextView textQuizTitle;
    private TextView textQuizContent;
    private TextView textQuizEmpty;
    private TextView textScoreEmpty;
    private Button btnTabActive;
    private Button btnTabHistory;
    private LinearLayout cardQuiz;
    private LinearLayout cardScores;
    private LinearLayout questionContainer;
    private LinearLayout scoreContainer;
    private Button btnSubmit;

    private LocalStorage localStorage;
    private int companyId;
    private int employeeId;
    private String employeeCode = "";
    private int currentQuizId = 0;
    private boolean alreadySubmitted = false;
    private boolean showingActiveSection = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_p5m, container, false);

        progress = view.findViewById(R.id.progress);
        p5mScroll = view.findViewById(R.id.p5mScroll);
        statusCard = view.findViewById(R.id.statusCard);
        textStatusBadge = view.findViewById(R.id.textStatusBadge);
        textStatus = view.findViewById(R.id.textStatus);
        textStatusSubtitle = view.findViewById(R.id.textStatusSubtitle);
        textQuizSource = view.findViewById(R.id.textQuizSource);
        textQuizTitle = view.findViewById(R.id.textQuizTitle);
        textQuizContent = view.findViewById(R.id.textQuizContent);
        textQuizEmpty = view.findViewById(R.id.textQuizEmpty);
        textScoreEmpty = view.findViewById(R.id.textScoreEmpty);
        btnTabActive = view.findViewById(R.id.btnTabActive);
        btnTabHistory = view.findViewById(R.id.btnTabHistory);
        cardQuiz = view.findViewById(R.id.cardQuiz);
        cardScores = view.findViewById(R.id.cardScores);
        questionContainer = view.findViewById(R.id.questionContainer);
        scoreContainer = view.findViewById(R.id.scoreContainer);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        localStorage = new LocalStorage(view.getContext());
        restoreEmployeeContext();

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((HomeActivity) getActivity()).changeViewPagerPostition(0);
            }
        });

        ImageButton btnReload = view.findViewById(R.id.btnReload);
        btnReload.setOnClickListener(v -> fetchP5m());

        btnTabActive.setOnClickListener(v -> switchSection(true));
        btnTabHistory.setOnClickListener(v -> switchSection(false));
        btnSubmit.setOnClickListener(v -> submitP5m());

        configureScrollBehavior();
        switchSection(true);
        applyP5MStatusStyle(P5MStatusState.LOADING);

        bindLoadingState(true);
        fetchP5m();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchP5m();
    }

    private void restoreEmployeeContext() {
        String storedEmployee = localStorage.getEmployee();
        if (storedEmployee == null || storedEmployee.isBlank()) {
            return;
        }

        try {
            JSONObject jsonEmployee = new JSONObject(storedEmployee);
            employeeCode = jsonEmployee.optString("code", "");
            companyId = jsonEmployee.optInt("company_id", 0);
            employeeId = jsonEmployee.optInt("id", 0);
        } catch (JSONException e) {
            employeeCode = "";
            companyId = 0;
            employeeId = 0;
        }
    }

    private void bindMissingContext() {
        bindLoadingState(false);
        currentQuizId = 0;
        alreadySubmitted = false;
        answerGroups.clear();
        questionContainer.removeAllViews();
        scoreContainer.removeAllViews();
        textQuizSource.setVisibility(View.GONE);
        textQuizTitle.setText(getString(R.string.p5m_title));
        textQuizContent.setText(getString(R.string.p5m_context_missing));
        textQuizEmpty.setVisibility(View.VISIBLE);
        textQuizEmpty.setText(getString(R.string.p5m_context_missing));
        textScoreEmpty.setVisibility(View.VISIBLE);
        textStatus.setText(getString(R.string.p5m_context_missing));
        textStatusSubtitle.setText(getString(R.string.p5m_status_subtitle_error));
        applyP5MStatusStyle(P5MStatusState.ERROR);
        btnSubmit.setEnabled(false);
        btnSubmit.setVisibility(View.VISIBLE);
    }

    private void fetchP5m() {
        if (!isAdded() || !requestRunning.compareAndSet(false, true)) {
            return;
        }

        if (companyId <= 0 || employeeId <= 0) {
            requestRunning.set(false);
            bindMissingContext();
            return;
        }

        bindLoadingState(true);
        String url = getString(R.string.base_url) + "/p5m";
        Context context = getContext();
        if (context == null) {
            requestRunning.set(false);
            bindLoadingState(false);
            return;
        }

        new Thread(() -> {
            Http http = new Http(context, url);
            http.setMethod("get");
            http.setToken(true);
            http.setBypassCache(true);
            http.send();

            Activity activity = getActivity();
            if (activity == null) {
                requestRunning.set(false);
                return;
            }

            activity.runOnUiThread(() -> {
                requestRunning.set(false);
                bindLoadingState(false);
                handleFetchResponse(http);
            });
        }).start();
    }

    private void handleFetchResponse(Http http) {
        int statusCode = http.getStatusCode() == null ? 0 : http.getStatusCode();
        if (statusCode == 200) {
            try {
                JSONObject response = new JSONObject(Objects.requireNonNullElse(http.getResponse(), "{}"));
                JSONObject data = response.optJSONObject("data");
                if (data == null) {
                    bindError(getString(R.string.p5m_error_invalid_response));
                    return;
                }
                bindState(data);
            } catch (JSONException e) {
                bindError(getString(R.string.p5m_error_invalid_response));
            }
            return;
        }

        bindHttpError(http, R.string.p5m_error_load);
    }

    private void bindState(JSONObject data) throws JSONException {
        answerGroups.clear();
        questionByItemId.clear();
        questionContainer.removeAllViews();
        scoreContainer.removeAllViews();
        currentQuizId = 0;

        alreadySubmitted = data.optBoolean("already_submitted", false);
        JSONObject todayScore = data.optJSONObject("today_score");
        if (alreadySubmitted || todayScore != null) {
            syncTodayMarker();
        }

        bindStatus(alreadySubmitted, todayScore);
        bindQuiz(data.optJSONObject("quiz"), data.optJSONArray("items"));
        bindScores(data.optJSONArray("scores"));
        applySectionVisibility();
    }

    private void bindStatus(boolean submittedToday, JSONObject todayScore) {
        if (submittedToday && todayScore != null) {
            textStatus.setText(getString(R.string.p5m_status_submitted_hidden_active));
            textStatusSubtitle.setText(getString(R.string.p5m_status_subtitle_submitted));
            applyP5MStatusStyle(P5MStatusState.SUBMITTED);
            return;
        }

        if (submittedToday) {
            textStatus.setText(getString(R.string.p5m_status_submitted_hidden_active));
            textStatusSubtitle.setText(getString(R.string.p5m_status_subtitle_submitted));
            applyP5MStatusStyle(P5MStatusState.SUBMITTED);
            return;
        }

        textStatus.setText(getString(R.string.p5m_status_pending));
        textStatusSubtitle.setText(getString(R.string.p5m_status_subtitle_pending));
        applyP5MStatusStyle(P5MStatusState.PENDING);
    }

    private void bindQuiz(JSONObject quiz, JSONArray items) throws JSONException {
        btnSubmit.setVisibility(View.VISIBLE);

        if (quiz == null) {
            textQuizSource.setVisibility(View.GONE);
            textQuizTitle.setText(getString(R.string.p5m_no_quiz_title));
            textQuizContent.setText("");
            textQuizEmpty.setText(getString(R.string.p5m_no_quiz));
            textQuizEmpty.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);
            return;
        }

        currentQuizId = quiz.optInt("id", 0);
        textQuizTitle.setText(quiz.optString("title", getString(R.string.p5m_title)));

        String content = quiz.optString("content", "");
        textQuizContent.setText(renderHtmlContent(content));
        textQuizContent.setVisibility(TextUtils.isEmpty(content) ? View.GONE : View.VISIBLE);

        textQuizSource.setVisibility(View.VISIBLE);
        textQuizSource.setText(resolveQuizSourceText(quiz));

        if (items == null || items.length() == 0) {
            textQuizEmpty.setText(alreadySubmitted
                    ? getString(R.string.p5m_quiz_completed_hint)
                    : getString(R.string.p5m_no_question));
            textQuizEmpty.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);
            btnSubmit.setVisibility(alreadySubmitted ? View.GONE : View.VISIBLE);
            return;
        }

        textQuizEmpty.setVisibility(View.GONE);
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            addQuestionView(i + 1, item);
        }

        btnSubmit.setEnabled(!alreadySubmitted && currentQuizId > 0);
        btnSubmit.setVisibility(alreadySubmitted ? View.GONE : View.VISIBLE);
        btnSubmit.setText(alreadySubmitted ? getString(R.string.p5m_submit_done) : getString(R.string.p5m_submit));
    }

    private void bindScores(JSONArray scores) {
        scoreContainer.removeAllViews();
        if (scores == null || scores.length() == 0) {
            textScoreEmpty.setVisibility(View.VISIBLE);
            return;
        }

        textScoreEmpty.setVisibility(View.GONE);
        for (int i = 0; i < scores.length(); i++) {
            JSONObject row = scores.optJSONObject(i);
            if (row == null) {
                continue;
            }
            scoreContainer.addView(createScoreView(row, i == 0));
        }
    }

    private void addQuestionView(int number, JSONObject item) throws JSONException {
        int itemId = item.optInt("id", 0);
        String question = item.optString("question", "");
        JSONArray options = item.optJSONArray("options");
        questionByItemId.put(itemId, question);

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_box);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = dp(10);
        card.setLayoutParams(cardParams);

        TextView textQuestion = new TextView(requireContext());
        textQuestion.setText(getString(R.string.p5m_question_format, number, question));
        textQuestion.setTextColor(Color.parseColor("#111111"));
        textQuestion.setTextSize(14);
        textQuestion.setTypeface(textQuestion.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(textQuestion);

        RadioGroup group = new RadioGroup(requireContext());
        group.setOrientation(RadioGroup.VERTICAL);
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        groupParams.topMargin = dp(8);
        group.setLayoutParams(groupParams);

        if (options != null) {
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.optJSONObject(i);
                if (option == null) {
                    continue;
                }

                RadioButton radioButton = new RadioButton(requireContext());
                radioButton.setId(View.generateViewId());
                radioButton.setTag(option.optString("key", ""));
                radioButton.setText(option.optString("key", "") + ". " + option.optString("label", ""));
                radioButton.setTextColor(Color.parseColor("#333333"));
                radioButton.setTextSize(12);
                radioButton.setEnabled(!alreadySubmitted);
                group.addView(radioButton);
            }
        }

        answerGroups.put(itemId, group);
        card.addView(group);
        questionContainer.addView(card);
    }

    private View createScoreView(JSONObject row, boolean first) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.bg_box);
        layout.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (!first) {
            params.topMargin = dp(10);
        }
        layout.setLayoutParams(params);

        TextView title = new TextView(requireContext());
        title.setText(row.optString("quiz_title", getString(R.string.p5m_score_unknown_title)));
        title.setTextColor(Color.parseColor("#111111"));
        title.setTextSize(13);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        layout.addView(title);

        TextView userMeta = new TextView(requireContext());
        userMeta.setText(getString(
            R.string.p5m_score_user_meta,
            row.optString("fullname", "-"),
            row.optString("code", "-")
        ));
        userMeta.setTextColor(Color.parseColor("#4B5563"));
        userMeta.setTextSize(11);
        LinearLayout.LayoutParams userMetaParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        userMetaParams.topMargin = dp(4);
        userMeta.setLayoutParams(userMetaParams);
        layout.addView(userMeta);

        TextView meta = new TextView(requireContext());
        meta.setText(getString(
                R.string.p5m_score_item_meta,
                row.optString("date", "-"),
                String.valueOf(row.optInt("score", 0))
        ));
        meta.setTextColor(Color.parseColor("#4B5563"));
        meta.setTextSize(11);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(4);
        meta.setLayoutParams(metaParams);
        layout.addView(meta);

        return layout;
    }

    private void submitP5m() {
        if (!isAdded() || requestRunning.get()) {
            return;
        }

        if (alreadySubmitted) {
            toastMessage(getString(R.string.p5m_status_already_submitted));
            return;
        }

        if (currentQuizId <= 0) {
            toastMessage(getString(R.string.p5m_no_quiz));
            return;
        }

        JSONArray answers = new JSONArray();
        JSONArray ticketAnswers = new JSONArray();
        for (Map.Entry<Integer, RadioGroup> entry : answerGroups.entrySet()) {
            int checkedId = entry.getValue().getCheckedRadioButtonId();
            if (checkedId == View.NO_ID) {
                toastMessage(getString(R.string.p5m_error_answer_all));
                return;
            }

            RadioButton selected = entry.getValue().findViewById(checkedId);
            JSONObject answer = new JSONObject();
            try {
                answer.put("id", entry.getKey());
                answer.put("value", String.valueOf(selected.getTag()));
                answers.put(answer);

                JSONObject ticketAnswer = new JSONObject();
                ticketAnswer.put("id", entry.getKey());
                ticketAnswer.put("question", questionByItemId.getOrDefault(entry.getKey(), ""));
                ticketAnswer.put("answer_key", String.valueOf(selected.getTag()));
                ticketAnswer.put("answer_label", selected.getText() == null ? "" : selected.getText().toString());
                ticketAnswers.put(ticketAnswer);
            } catch (JSONException e) {
                toastMessage(getString(R.string.p5m_error_invalid_response));
                return;
            }
        }
        final String cachedTicketAnswers = buildTicketAnswerCache(ticketAnswers);

        JSONObject payload = new JSONObject();
        try {
            payload.put("quiz_id", currentQuizId);
            payload.put("answer", answers);
        } catch (JSONException e) {
            toastMessage(getString(R.string.p5m_error_invalid_response));
            return;
        }

        if (!requestRunning.compareAndSet(false, true)) {
            return;
        }

        bindLoadingState(true);
        String url = getString(R.string.base_url) + "/p5m";
        Context context = getContext();
        if (context == null) {
            requestRunning.set(false);
            bindLoadingState(false);
            return;
        }

        new Thread(() -> {
            Http http = new Http(context, url);
            http.setMethod("post");
            http.setData(payload.toString());
            http.setToken(true);
            http.send();

            Activity activity = getActivity();
            if (activity == null) {
                requestRunning.set(false);
                return;
            }

            activity.runOnUiThread(() -> {
                requestRunning.set(false);
                bindLoadingState(false);
                handleSubmitResponse(http, cachedTicketAnswers);
            });
        }).start();
    }

    private void handleSubmitResponse(Http http, String cachedTicketAnswers) {
        int statusCode = http.getStatusCode() == null ? 0 : http.getStatusCode();
        if (statusCode == 200) {
            try {
                JSONObject response = new JSONObject(Objects.requireNonNullElse(http.getResponse(), "{}"));
                String message = response.optString("message", getString(R.string.p5m_submit_success));
                if ("Successfully created".equalsIgnoreCase(message) || "Already submitted".equalsIgnoreCase(message)) {
                    if (cachedTicketAnswers != null && !cachedTicketAnswers.isBlank()) {
                        localStorage.setP5MAnswers(cachedTicketAnswers);
                    }
                    syncTodayMarker();
                    alreadySubmitted = true;
                    toastMessage("Already submitted".equalsIgnoreCase(message)
                            ? getString(R.string.p5m_status_already_submitted)
                            : getString(R.string.p5m_submit_success));
                    navigateToHome();
                    return;
                }
                toastMessage(message);
            } catch (JSONException e) {
                bindError(getString(R.string.p5m_error_invalid_response));
            }
            return;
        }

        bindHttpError(http, R.string.p5m_error_submit);
    }

    private void bindHttpError(Http http, int fallbackResId) {
        try {
            JSONObject response = new JSONObject(Objects.requireNonNullElse(http.getResponse(), "{}"));
            String message = normalizeBackendMessage(response, fallbackResId);
            bindError(message);
        } catch (JSONException e) {
            bindError(getString(fallbackResId));
        }
    }

    private String normalizeBackendMessage(JSONObject response, int fallbackResId) {
        JSONObject errors = response.optJSONObject("errors");
        if (errors != null && errors.has("answer")) {
            return getString(R.string.p5m_error_answer_all);
        }

        String message = response.optString("message", "");
        if (message.toLowerCase(Locale.ROOT).contains("answer field must have at least 2 items")) {
            return getString(R.string.p5m_error_answer_all);
        }

        if (!message.isBlank()) {
            return message;
        }

        return getString(fallbackResId);
    }

    private Spanned renderHtmlContent(String rawHtml) {
        return HtmlCompat.fromHtml(rawHtml == null ? "" : rawHtml, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    private void bindError(String message) {
        textStatus.setText(message);
        textStatusSubtitle.setText(getString(R.string.p5m_status_subtitle_error));
        applyP5MStatusStyle(P5MStatusState.ERROR);
        toastMessage(message);
    }

    private void bindLoadingState(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        p5mScroll.setAlpha(loading ? 0.55f : 1f);
        if (loading) {
            textStatus.setText(getString(R.string.p5m_status_loading));
            textStatusSubtitle.setText(getString(R.string.p5m_status_subtitle_loading));
            applyP5MStatusStyle(P5MStatusState.LOADING);
        }
        btnSubmit.setEnabled(!loading && !alreadySubmitted && currentQuizId > 0);
    }

    private void configureScrollBehavior() {
        p5mScroll.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
            }
            return false;
        });
    }

    private void switchSection(boolean showActive) {
        showingActiveSection = showActive;

        applySectionVisibility();

        btnTabActive.setEnabled(!showActive);
        btnTabHistory.setEnabled(showActive);

        btnTabActive.setBackgroundResource(showActive ? R.drawable.bg_premium_panel : R.drawable.bg_box);
        btnTabHistory.setBackgroundResource(showActive ? R.drawable.bg_box : R.drawable.bg_premium_panel);

        btnTabActive.setTextColor(showActive ? Color.parseColor("#FFFFFF") : Color.parseColor("#111111"));
        btnTabHistory.setTextColor(showActive ? Color.parseColor("#111111") : Color.parseColor("#FFFFFF"));
    }

    private void applySectionVisibility() {
        statusCard.setVisibility(showingActiveSection ? View.VISIBLE : View.GONE);
        cardQuiz.setVisibility(showingActiveSection && !alreadySubmitted ? View.VISIBLE : View.GONE);
        cardScores.setVisibility(showingActiveSection ? View.GONE : View.VISIBLE);
    }

    private void applyP5MStatusStyle(P5MStatusState state) {
        switch (state) {
            case SUBMITTED:
                statusCard.setBackgroundResource(R.drawable.bg_p5m_status_success);
                textStatusBadge.setBackgroundResource(R.drawable.bg_p5m_badge_success);
                textStatusBadge.setText("OK");
                textStatusBadge.setTextColor(Color.parseColor("#15803D"));
                textStatus.setTextColor(Color.parseColor("#166534"));
                textStatusSubtitle.setTextColor(Color.parseColor("#15803D"));
                break;
            case PENDING:
                statusCard.setBackgroundResource(R.drawable.bg_p5m_status_pending);
                textStatusBadge.setBackgroundResource(R.drawable.bg_p5m_badge_pending);
                textStatusBadge.setText("!");
                textStatusBadge.setTextColor(Color.parseColor("#C2410C"));
                textStatus.setTextColor(Color.parseColor("#9A3412"));
                textStatusSubtitle.setTextColor(Color.parseColor("#C2410C"));
                break;
            case ERROR:
                statusCard.setBackgroundResource(R.drawable.bg_p5m_status_error);
                textStatusBadge.setBackgroundResource(R.drawable.bg_p5m_badge_error);
                textStatusBadge.setText("!");
                textStatusBadge.setTextColor(Color.parseColor("#B91C1C"));
                textStatus.setTextColor(Color.parseColor("#991B1B"));
                textStatusSubtitle.setTextColor(Color.parseColor("#B91C1C"));
                break;
            case LOADING:
            default:
                statusCard.setBackgroundResource(R.drawable.bg_p5m_status_info);
                textStatusBadge.setBackgroundResource(R.drawable.bg_p5m_badge_info);
                textStatusBadge.setText("P5");
                textStatusBadge.setTextColor(Color.parseColor("#1D4ED8"));
                textStatus.setTextColor(Color.parseColor("#1E3A8A"));
                textStatusSubtitle.setTextColor(Color.parseColor("#1D4ED8"));
                break;
        }
    }

    private String resolveQuizSourceText(JSONObject quiz) {
        String source = quiz.optString("source", "today");
        String sourceDate = quiz.optString("source_date", "");
        if ("yesterday".equalsIgnoreCase(source)) {
            return getString(R.string.p5m_source_yesterday, sourceDate);
        }
        if ("latest_active".equalsIgnoreCase(source)) {
            return getString(R.string.p5m_source_latest, sourceDate);
        }
        return getString(R.string.p5m_source_today, sourceDate);
    }

    private void syncTodayMarker() {
        if (employeeCode == null || employeeCode.isBlank()) {
            return;
        }

        String today = new SimpleDateFormat("E, dd MMM yyyy", Locale.getDefault()).format(System.currentTimeMillis());
        localStorage.setP5M(employeeCode + "_" + today);
    }

    private String buildTicketAnswerCache(JSONArray ticketAnswers) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("quiz_id", currentQuizId);
            payload.put("submitted_at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(System.currentTimeMillis()));
            payload.put("answers", ticketAnswers);
            return payload.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    private void toastMessage(String message) {
        if (!isAdded()) {
            return;
        }
        GB.toast(requireContext(), message, Toast.LENGTH_SHORT, GB.INFO);
    }

    private void navigateToHome() {
        Activity activity = getActivity();
        if (activity instanceof HomeActivity) {
            ((HomeActivity) activity).changeViewPagerPostition(0);
        }
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    private enum P5MStatusState {
        LOADING,
        SUBMITTED,
        PENDING,
        ERROR
    }
}
