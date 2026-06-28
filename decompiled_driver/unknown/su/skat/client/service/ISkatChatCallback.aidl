package su.skat.client.service;

import su.skat.client.model.ChatMessage;
import su.skat.client.model.ChatChannel;

interface ISkatChatCallback {
    void updateChannel(in ChatChannel channel);
    void updateMessage(in ChatMessage message);
    void removeMessage(String channelId, String messageId);
}