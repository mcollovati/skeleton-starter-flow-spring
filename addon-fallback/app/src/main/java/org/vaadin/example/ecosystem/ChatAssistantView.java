package org.vaadin.example.ecosystem;

import com.flowingcode.vaadin.addons.chatassistant.ChatAssistant;
import com.flowingcode.vaadin.addons.chatassistant.model.Message;
import com.vaadin.flow.router.Route;

/**
 * ChatAssistant (org.vaadin.addons.flowingcode:chat-assistant-addon 5.2.0).
 */
@Route("eco/chat-assistant")
public class ChatAssistantView extends EcoView {

    public ChatAssistantView() {
        super("ChatAssistant");
        run(() -> {
            ChatAssistant<Message> chat = withId(new ChatAssistant<>(), "chat");
            add(chat);
            chat.sendMessage(Message.builder().name("Bot")
                    .content("Hello CSP").build());
            add(button("open", "Open", () -> chat.setOpened(true)));
        });
    }
}
