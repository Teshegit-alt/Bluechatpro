package com.example.bluechatpro.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bluechatpro.R;
import com.example.bluechatpro.models.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;

    private List<ChatMessage> messageList;

    public MessageAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);

        if (message.getSenderName().equals("System")) {
            return VIEW_TYPE_SYSTEM;
        } else if (message.isSent()) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_system, parent, false);
            return new SystemMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof SentMessageHolder) {
            ((SentMessageHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageHolder) {
            ((ReceivedMessageHolder) holder).bind(message);
        } else if (holder instanceof SystemMessageHolder) {
            ((SystemMessageHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Update the dataset
    public void updateMessages(List<ChatMessage> newMessages) {
        messageList = newMessages;
        notifyDataSetChanged();
    }

    // Add a message
    public void addMessage(ChatMessage message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    // Clear all messages
    public void clearMessages() {
        messageList.clear();
        notifyDataSetChanged();
    }

    // ViewHolder for sent messages
    static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;
        TextView textStatus;

        SentMessageHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getContent());
            textTime.setText(message.getFormattedTime());

            // Set status indicator
            if (textStatus != null) {
                switch (message.getStatus()) {
                    case SENDING:
                        textStatus.setText("Sending...");
                        textStatus.setVisibility(View.VISIBLE);
                        break;
                    case SENT:
                        textStatus.setText("Sent");
                        textStatus.setVisibility(View.VISIBLE);
                        break;
                    case DELIVERED:
                        textStatus.setText("Delivered");
                        textStatus.setVisibility(View.VISIBLE);
                        break;
                    case READ:
                        textStatus.setText("Read");
                        textStatus.setVisibility(View.VISIBLE);
                        break;
                    case FAILED:
                        textStatus.setText("Failed");
                        textStatus.setVisibility(View.VISIBLE);
                        break;
                    default:
                        textStatus.setVisibility(View.GONE);
                }
            }
        }
    }

    // ViewHolder for received messages
    static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView textSender;
        TextView textMessage;
        TextView textTime;

        ReceivedMessageHolder(@NonNull View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.textSender);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }

        void bind(ChatMessage message) {
            if (textSender != null) {
                textSender.setText(message.getSenderName());
            }
            textMessage.setText(message.getContent());
            textTime.setText(message.getFormattedTime());
        }
    }

    // ViewHolder for system messages
    static class SystemMessageHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;

        SystemMessageHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getContent());
            textTime.setText(message.getFormattedTime());
        }
    }
}