package com.example.ywinked;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;
    private static final int VIEW_TYPE_THINKING = 3;

    private ArrayList<MessageModel> messages;
    private Markwon markwon;
    private OnMessageSpeakListener speakListener;
    private OnMessageLongClickListener longClickListener;

    public interface OnMessageSpeakListener {
        void onSpeak(String text);
    }

    public interface OnMessageLongClickListener {
        void onLongClick(MessageModel message, int position);
    }

    public ChatAdapter(ArrayList<MessageModel> messages, OnMessageSpeakListener speakListener, OnMessageLongClickListener longClickListener) {
        this.messages = messages;
        this.speakListener = speakListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messages.get(position);
        if (message.isThinking()) {
            return VIEW_TYPE_THINKING;
        }
        return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_message, parent, false);
            return new UserViewHolder(view);
        } else if (viewType == VIEW_TYPE_THINKING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_thinking_message, parent, false);
            return new ThinkingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bot_message, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvMessage.setText(message.getMessage());
            userHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onLongClick(message, holder.getAdapterPosition());
                    return true;
                }
                return false;
            });
        } else if (holder instanceof ThinkingViewHolder) {
            ((ThinkingViewHolder) holder).startAnimation();
        } else if (holder instanceof BotViewHolder) {
            BotViewHolder botHolder = (BotViewHolder) holder;
            if (markwon == null) {
                markwon = Markwon.create(holder.itemView.getContext());
            }
            markwon.setMarkdown(botHolder.tvMessage, message.getMessage());
            botHolder.btnSpeak.setOnClickListener(v -> {
                if (speakListener != null) {
                    speakListener.onSpeak(message.getMessage());
                }
            });
            botHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onLongClick(message, holder.getAdapterPosition());
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ThinkingViewHolder) {
            ((ThinkingViewHolder) holder).stopAnimation();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        android.widget.ImageButton btnSpeak;
        public BotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvBotMessage);
            btnSpeak = itemView.findViewById(R.id.btnSpeakMessage);
        }
    }

    static class ThinkingViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        private final android.os.Handler handler = new android.os.Handler();
        private int dotCount = 1;
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                StringBuilder text = new StringBuilder("Thinking");
                for (int i = 0; i < dotCount; i++) {
                    text.append(".");
                }
                tvMessage.setText(text.toString());
                dotCount = (dotCount % 3) + 1;
                handler.postDelayed(this, 500);
            }
        };

        public ThinkingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvThinkingMessage);
        }

        void startAnimation() {
            handler.removeCallbacks(runnable);
            handler.post(runnable);
        }

        void stopAnimation() {
            handler.removeCallbacks(runnable);
        }
    }
}
