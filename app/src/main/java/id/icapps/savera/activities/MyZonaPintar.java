package id.icapps.savera.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import id.icapps.savera.Http;
import id.icapps.savera.LocalStorage;
import id.icapps.savera.R;
import id.icapps.savera.adapter.ArticleAdapter;
import id.icapps.savera.model.Article;

public class MyZonaPintar extends Fragment {
    private static final Logger LOG = LoggerFactory.getLogger(MyZonaPintar.class);
    
    private RecyclerView recyclerView;
    private ArticleAdapter articleAdapter;
    private List<Article> articleList;
    private ProgressBar progress;
    private LinearLayout emptyState;
    private LocalStorage localStorage;
    private int companyId;
    private int employeeId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_zonapintar, container, false);

        // Initialize views
        recyclerView = view.findViewById(R.id.articlesRecyclerView);
        progress = view.findViewById(R.id.progress);
        emptyState = view.findViewById(R.id.emptyState);

        // Setup RecyclerView
        articleList = new ArrayList<>();
        articleAdapter = new ArticleAdapter(getContext(), articleList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(articleAdapter);

        // Get user data
        localStorage = new LocalStorage(view.getContext());
        if (!localStorage.getEmployee().isEmpty() && !localStorage.getEmployee().isBlank()) {
            try {
                JSONObject jsonEmployee = new JSONObject(localStorage.getEmployee());
                companyId = jsonEmployee.optInt("company_id", 0);
                employeeId = jsonEmployee.optInt("id", 0);
            } catch (JSONException e) {
                LOG.error("Error parsing employee data", e);
            }
        }

        // Back button
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((HomeActivity) getActivity()).changeViewPagerPostition(0);
            }
        });

        // Reload button
        ImageButton btnReload = view.findViewById(R.id.btnReload);
        btnReload.setOnClickListener(v -> loadArticles());

        // Load articles
        loadArticles();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadArticles();
    }

    private void loadArticles() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        if (companyId <= 0 || employeeId <= 0) {
            LOG.warn("Skipping articles load: invalid employee context companyId={} employeeId={}", companyId, employeeId);
            progress.setVisibility(View.GONE);
            showEmptyState();
            return;
        }

        // API endpoint: /api/articles?company_id=X&employee_id=Y&type=Zona+Operator+Pintar
        String url = getString(R.string.base_url) + "/articles?company_id=" + companyId + "&employee_id=" + employeeId + "&type=Zona+Operator+Pintar";

        Context context = getContext();
        if (context == null) {
            progress.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        new Thread(() -> {
            Http http = new Http(context, url);
            http.setMethod("get");
            http.setToken(true);
            http.setBypassCache(true);
            http.send();

            if (getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed()) {
                getActivity().runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    
                    Integer code = http.getStatusCode();
                    if (code == 200) {
                        try {
                            JSONObject response = new JSONObject(http.getResponse());
                            LOG.debug("API Response: " + response.toString());
                            
                            // Parse articles array
                            // Expected format: {"status": "success", "data": [...]}
                            JSONArray articlesArray = response.optJSONArray("data");
                            
                            if (articlesArray != null && articlesArray.length() > 0) {
                                LOG.debug("Articles array length: " + articlesArray.length());
                                List<Article> articles = parseArticles(articlesArray);
                                articleList.clear();
                                articleList.addAll(articles);
                                articleAdapter.notifyDataSetChanged();
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyState.setVisibility(View.GONE);
                            } else {
                                // No articles found
                                LOG.debug("No articles found in response");
                                showEmptyState();
                            }
                            
                        } catch (JSONException e) {
                            LOG.error("Error parsing articles response", e);
                            showEmptyState();
                        }
                    } else {
                        // API error or no response
                        LOG.error("Failed to load articles, status code: " + code);
                        showEmptyState();
                    }
                });
            }
        }).start();
    }

    private List<Article> parseArticles(JSONArray articlesArray) throws JSONException {
        List<Article> articles = new ArrayList<>();
        
        for (int i = 0; i < articlesArray.length(); i++) {
            JSONObject articleObj = articlesArray.getJSONObject(i);
            
            Article article = new Article();
            String category = getSafeString(articleObj, "category", "");
            if (category.isEmpty()) {
                category = getSafeString(articleObj, "type", "Zona Operator Pintar");
            }

            article.setId(articleObj.optInt("id", 0));
            article.setTitle(getSafeString(articleObj, "title", ""));
            article.setContent(getSafeString(articleObj, "content", ""));
            article.setImageUrl(getSafeString(articleObj, "image_url", ""));
            article.setCategory(category);
            article.setPublishedDate(getSafeString(articleObj, "published_date", ""));
            article.setAuthor(getSafeString(articleObj, "author", "Pembuat Artikel"));
            article.setUpdatedAt(getSafeString(articleObj, "updated_at", ""));
            
            LOG.debug("Parsed Article [" + i + "]: ID=" + article.getId() + 
                    " | Title=" + article.getTitle() + 
                    " | ImageUrl=" + article.getImageUrl());
            
            articles.add(article);
        }
        
        // Sort by updated_at descending (latest update first)
        Collections.sort(articles, (a, b) -> {
            String ua = a.getUpdatedAt() != null ? a.getUpdatedAt() : "";
            String ub = b.getUpdatedAt() != null ? b.getUpdatedAt() : "";
            return ub.compareTo(ua);
        });
        
        return articles;
    }

    private String getSafeString(JSONObject object, String key, String defaultValue) {
        String value = object.optString(key, defaultValue);
        if (value == null) {
            return defaultValue;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return defaultValue;
        }

        return trimmed;
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
}
