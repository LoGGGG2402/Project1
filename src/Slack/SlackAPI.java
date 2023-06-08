package Slack;


import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.slack.api.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class SlackAPI {

    private static final String token = "xoxb-5244767721591-5326693653700-351dqpn8DUCgFiYFwT5gN9k4";

    private static final Logger logger = LoggerFactory.getLogger("my-slack-app");

    public List<User> listUser() {
        var client = Slack.getInstance().methods();

        try {
            var result = client.usersList(r -> r.token(token));

            if (result.isOk()) {
                return result.getMembers();
            } else {
                logger.error("error: {}", result.getError());
                return null;
            }
        } catch (SlackApiException | IOException e) {
            logger.error("error: {}", e.getMessage(), e);
        }
        return null;
    }
    public List<Conversation> listChannel() {
        var client = Slack.getInstance().methods();

        try {
            var result = client.conversationsList(r -> r
                    .token(token)
                    .limit(1000)
            );

            if (result.isOk()) {
                return result.getChannels();
            } else {
                logger.error("error: {}", result.getError());
                return null;
            }
        } catch (SlackApiException | IOException e) {
            logger.error("error: {}", e.getMessage(), e);
        }
        return null;
    }
    public List<Conversation> listChannel(String user) {
        var client = Slack.getInstance().methods();

        try {
            var result = client.conversationsList(r -> r
                    .token(token)
                    .limit(1000)
                    .types(List.of(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL, ConversationType.IM, ConversationType.MPIM))
                    .excludeArchived(false)
                    .limit(1000)
            );

            if (result.isOk()) {
                return result.getChannels();
            } else {
                logger.error("error: {}", result.getError());
                return null;
            }
        } catch (SlackApiException | IOException e) {
            logger.error("error: {}", e.getMessage(), e);
        }
        return null;
    }
    public void addUserToChannel(List<String> usersId, String channelId){
        var client = Slack.getInstance().methods();

        try {
            var result = client.conversationsInvite(r -> r
                    .token(token)
                    .channel(channelId)
                    .users(usersId)
            );

            if (result.isOk()) {
                System.out.println("Add user " + usersId + " to channel " + channelId + " success");
            } else {
                logger.error("error: {}", result.getError());
            }
        } catch (SlackApiException | IOException e) {
            logger.error("error: {}", e.getMessage(), e);
        }
    }

    public void createChannel(String name){
        var client = Slack.getInstance().methods();

        try {
            var result = client.conversationsCreate(r -> r
                    .token(token)
                    .name(name)
            );
            if (result.isOk()){
                System.out.println("Create channel " + name + " success");
            }else {
                logger.error("error: {}", result.getError());
            }
        } catch (SlackApiException | IOException e) {
            logger.error("error: {}", e.getMessage(), e);
        }
    }
}