package id.icapps.savera.activities;

import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import id.icapps.savera.R;
import id.icapps.savera.model.Article;

public class ArticleDetailActivity extends AppCompatActivity {

    private ImageView articleImage;
    private TextView articleTitle;
    private TextView articleCategory;
    private TextView articleAuthor;
    private TextView articleDate;
    private TextView articleContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        // This screen uses a custom header in XML, so hide ActionBar to avoid duplicate titles.
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        ImageButton btnBack = findViewById(R.id.btnBack);
        articleImage = findViewById(R.id.articleDetailImage);
        articleTitle = findViewById(R.id.articleDetailTitle);
        articleCategory = findViewById(R.id.articleDetailCategory);
        articleAuthor = findViewById(R.id.articleDetailAuthor);
        articleDate = findViewById(R.id.articleDetailDate);
        articleContent = findViewById(R.id.articleDetailContent);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Get article from intent
        Article article = (Article) getIntent().getSerializableExtra("article");
        if (article != null) {
            displayArticle(article);
        }
    }

    private void displayArticle(Article article) {
        articleTitle.setText(article.getTitle());
        articleCategory.setText(getDisplayValue(article.getCategory(), "Zona Operator Pintar"));
        articleAuthor.setText(getDisplayValue(article.getAuthor(), "Pembuat Artikel"));
        articleDate.setText(getDisplayValue(article.getPublishedDate(), ""));

        // Display content with HTML formatting
        if (article.getContent() != null) {
            articleContent.setText(Html.fromHtml(article.getContent(), Html.FROM_HTML_MODE_LEGACY));
        }

        // Load image from URL using Glide
        String imageUrl = article.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_zona_pintar)
                    .error(R.drawable.bg_zona_pintar)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(articleImage);
        } else {
            Glide.with(this)
                    .load(R.drawable.bg_zona_pintar)
                    .centerCrop()
                    .into(articleImage);
        }
    }

    private String getDisplayValue(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return fallback;
        }

        return trimmed;
    }
}
