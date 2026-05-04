package id.icapps.savera.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

import id.icapps.savera.R;
import id.icapps.savera.activities.ArticleDetailActivity;
import id.icapps.savera.model.Article;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {
    
    private Context context;
    private List<Article> articleList;

    public ArticleAdapter(Context context, List<Article> articleList) {
        this.context = context;
        this.articleList = articleList;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articleList.get(position);
        
        holder.titleTextView.setText(article.getTitle());
        holder.categoryTextView.setText(getDisplayValue(article.getCategory(), "Zona Operator Pintar"));
        holder.authorTextView.setText(getDisplayValue(article.getAuthor(), "Pembuat Artikel"));
        holder.dateTextView.setText(getDisplayValue(article.getPublishedDate(), ""));
        
        // Strip HTML tags for preview (max 120 chars)
        String preview = article.getContent();
        if (preview != null) {
            preview = Html.fromHtml(preview, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            if (preview.length() > 120) {
                preview = preview.substring(0, 120) + "…";
            }
            holder.previewTextView.setText(preview);
        }
        
        // Load image from URL using Glide
        String imageUrl = article.getImageUrl();
        android.util.Log.d("ArticleAdapter", "Article ID: " + article.getId() + " | ImageURL: " + imageUrl);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_zona_pintar)
                    .error(R.drawable.bg_zona_pintar)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imageView);
        } else {
            Glide.with(context)
                    .load(R.drawable.bg_zona_pintar)
                    .centerCrop()
                    .into(holder.imageView);
        }
        
        // Click listener to open detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ArticleDetailActivity.class);
            intent.putExtra("article", article);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return articleList.size();
    }

    public void updateArticles(List<Article> newArticles) {
        this.articleList = newArticles;
        notifyDataSetChanged();
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

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleTextView;
        TextView categoryTextView;
        TextView previewTextView;
        TextView authorTextView;
        TextView dateTextView;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.articleImage);
            titleTextView = itemView.findViewById(R.id.articleTitle);
            categoryTextView = itemView.findViewById(R.id.articleCategory);
            previewTextView = itemView.findViewById(R.id.articlePreview);
            authorTextView = itemView.findViewById(R.id.articleAuthor);
            dateTextView = itemView.findViewById(R.id.articleDate);
        }
    }
}
