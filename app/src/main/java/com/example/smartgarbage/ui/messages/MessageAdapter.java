package com.example.smartgarbage.ui.messages;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartgarbage.R;
import com.example.smartgarbage.data.model.Message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final int currentDriverId;
    private final List<Message> messages = new ArrayList<>();

    public MessageAdapter(int currentDriverId) {
        this.currentDriverId = currentDriverId;
    }

    public void setMessages(List<Message> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    public void appendMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() { return messages.size(); }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout layoutMessage;
        private final TextView tvContent;
        private final TextView tvTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutMessage = itemView.findViewById(R.id.layoutMessage);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }

        void bind(Message message) {
            boolean isMine = "driver".equals(message.getSenderRole())
                    && message.getSenderId() == currentDriverId;

            tvContent.setText(message.getContent());
            tvTime.setText(formatTime(message.getCreatedAt()));

            // Align right for own messages, left for admin messages
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layoutMessage.getLayoutParams();
            if (isMine) {
                params.gravity = Gravity.END;
                layoutMessage.setBackgroundResource(R.drawable.bg_message_sent);
            } else {
                params.gravity = Gravity.START;
                layoutMessage.setBackgroundResource(R.drawable.bg_message_received);
            }
            layoutMessage.setLayoutParams(params);
        }

        private String formatTime(String isoDate) {
            if (isoDate == null) return "";
            try {
                SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                isoFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = isoFmt.parse(isoDate);
                SimpleDateFormat outFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                return outFmt.format(date);
            } catch (ParseException e) {
                // Try without milliseconds
                try {
                    SimpleDateFormat isoFmt2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    isoFmt2.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = isoFmt2.parse(isoDate);
                    SimpleDateFormat outFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    return outFmt.format(date);
                } catch (ParseException ex) {
                    return "";
                }
            }
        }
    }
}