package in.ashwanthkumar.gocd.slack;

import static in.ashwanthkumar.utils.lang.StringUtils.startsWith;

import java.net.URISyntaxException;

import com.thoughtworks.go.plugin.api.logging.Logger;

import in.ashwanthkumar.gocd.slack.ruleset.PipelineRule;
import in.ashwanthkumar.gocd.slack.ruleset.PipelineStatus;
import in.ashwanthkumar.gocd.slack.ruleset.Rules;
import in.ashwanthkumar.slack.webhook.Slack;
import in.ashwanthkumar.slack.webhook.SlackAttachment;

public class SlackPipelineListener extends PipelineListener {
    private Logger LOG = Logger.getLoggerFor(SlackPipelineListener.class);

    private final Slack slack;

    public SlackPipelineListener(Rules rules) {
        super(rules);
        slack = new Slack(rules.getWebHookUrl());
        updateSlackChannel(rules.getSlackChannel());

        slack.displayName(rules.getSlackDisplayName())
            .icon(rules.getSlackUserIcon());
    }

    @Override
    public void onBuilding(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSlackChannel(rule.getChannel());
        slack.push(slackAttachment(message, PipelineStatus.BUILDING));
    }

    @Override
    public void onPassed(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSlackChannel(rule.getChannel());
        slack.push(slackAttachment(message, PipelineStatus.PASSED).color("good"));
    }

    @Override
    public void onFailed(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSlackChannel(rule.getChannel());
        slack.push(slackAttachment(message, PipelineStatus.FAILED).color("danger"));
    }

    @Override
    public void onBroken(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSlackChannel(rule.getChannel());
        slack.push(slackAttachment(message, PipelineStatus.BROKEN).color("danger"));
    }

    @Override
    public void onFixed(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSlackChannel(rule.getChannel());
        slack.push(slackAttachment(message, PipelineStatus.FIXED).color("good"));
    }

    @Override
    public void onCancelled(PipelineRule rule, GoNotificationMessage message) throws Exception {
        updateSlackChannel(rule.getChannel());
        slack.push(slackAttachment(message, PipelineStatus.CANCELLED).color("warning"));
    }

    private SlackAttachment slackAttachment(GoNotificationMessage message, PipelineStatus pipelineStatus) throws URISyntaxException {
        StringBuilder sb = new StringBuilder();

        sb.append("See details - ");
        sb.append(message.goServerUrl(rules.getGoServerHost()));
        sb.append("\n");

        return new SlackAttachment(sb.toString())
                .fallback(String.format("%s - %s %s %s", message.getStageName(), message.getPipelineName(), verbFor(pipelineStatus), pipelineStatus).replaceAll("\\s+", " "))
                .title(String.format("Stage \"%s\" of \"%s\" %s %s", message.getStageName(), message.getPipelineName(), verbFor(pipelineStatus), pipelineStatus).replaceAll("\\s+", " "));
    }

    private String verbFor(PipelineStatus pipelineStatus) {
        switch (pipelineStatus) {
            case BROKEN:
            	return "has";
            case FIXED:
                return "is";
            case FAILED:
            	return "has";
            case PASSED:
                return "has";
            case CANCELLED:
                return "was";
            case BUILDING:
            	return "is";
            default:
                return "";
        }
    }

    private void updateSlackChannel(String slackChannel) {
        LOG.debug(String.format("Updating target slack channel to %s", slackChannel));
        // by default post it to where ever the hook is configured to do so
        if (startsWith(slackChannel, "#")) {
            slack.sendToChannel(slackChannel.substring(1));
        } else if (startsWith(slackChannel, "@")) {
            slack.sendToUser(slackChannel.substring(1));
        }
    }
}
