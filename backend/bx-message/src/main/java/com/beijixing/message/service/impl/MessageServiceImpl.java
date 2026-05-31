package com.beijixing.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.message.dto.SendMessageRequest;
import com.beijixing.message.entity.ChatMessage;
import com.beijixing.message.mapper.ChatMessageMapper;
import com.beijixing.message.service.MessageService;
import com.beijixing.message.service.WebSocketService;
import com.beijixing.message.vo.MessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务实现类（统一技术栈 - MariaDB + MyBatis-Plus + Redis）
 *
 * @author 苏波（EMP-BE-001）
 */
@Slf4j
@Service
@SuppressWarnings("nullness")
public class MessageServiceImpl implements MessageService {

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private WebSocketService webSocketService;

    private static final Integer NOT_DELETED = 0;
    private static final Integer STATUS_SENT = 1;
    private static final Integer STATUS_READ = 2;

    @Override
    public MessageVO sendMessage(SendMessageRequest request, Long senderId, String senderName) {
        ChatMessage message = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .senderId(senderId)
                .senderName(senderName)
                .receiverId(request.getReceiverId())
                .receiverName(request.getReceiverName())
                .tenantId(request.getTenantId())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .mediaSize(request.getMediaSize())
                .mediaName(request.getMediaName())
                .leadId(request.getLeadId())
                .replyId(request.getReplyId())
                .status(STATUS_SENT)
                .deleted(NOT_DELETED)
                .createdTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        chatMessageMapper.insert(message);
        log.info("消息保存成功：messageId={}, senderId={}, receiverId={}", message.getId(), senderId, request.getReceiverId());

        MessageVO messageVO = toVO(message);

        try {
            webSocketService.pushToUser(request.getReceiverId(), messageVO);
        } catch (Exception e) {
            log.warn("WebSocket推送失败（非关键错误）：receiverId={}", request.getReceiverId(), e);
        }

        return messageVO;
    }

    @Override
    public Page<MessageVO> getSessionMessages(String sessionId, int page, int size) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .eq(ChatMessage::getDeleted, NOT_DELETED)
               .orderByDesc(ChatMessage::getCreatedTime);

        long total = chatMessageMapper.selectCount(wrapper);
        int start = (page - 1) * size;
        
        wrapper.last("LIMIT " + start + "," + size);
        List<ChatMessage> records = chatMessageMapper.selectList(wrapper);
        
        List<MessageVO> voList = records.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
                
        return new PageImpl<>(voList, PageRequest.of(page - 1, size), total);
    }

    @Override
    public Page<MessageVO> getPrivateMessages(Long userId, Long otherUserId, int page, int size) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, otherUserId))
               .or(o -> o.eq(ChatMessage::getSenderId, otherUserId).eq(ChatMessage::getReceiverId, userId))
               .eq(ChatMessage::getDeleted, NOT_DELETED)
               .orderByDesc(ChatMessage::getCreatedTime);

        long total = chatMessageMapper.selectCount(wrapper);
        int start = (page - 1) * size;
        
        wrapper.last("LIMIT " + start + "," + size);
        List<ChatMessage> records = chatMessageMapper.selectList(wrapper);
        
        List<MessageVO> voList = records.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
                
        return new PageImpl<>(voList, PageRequest.of(page - 1, size), total);
    }

    @Override
    public void markAsRead(String sessionId, Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .eq(ChatMessage::getReceiverId, userId)
               .eq(ChatMessage::getStatus, STATUS_SENT)
               .eq(ChatMessage::getDeleted, NOT_DELETED);

        List<ChatMessage> unreadMessages = chatMessageMapper.selectList(wrapper);
        for (ChatMessage message : unreadMessages) {
            message.setStatus(STATUS_READ);
            message.setUpdateTime(LocalDateTime.now());
            chatMessageMapper.updateById(message);
        }
        log.info("标记会话消息已读：sessionId={}, userId={}, count={}", sessionId, userId, unreadMessages.size());
    }

    @Override
    public boolean recallMessage(String messageId, Long userId) {
        ChatMessage message = chatMessageMapper.selectById(Long.parseLong(messageId));
        if (message != null && message.getSenderId().equals(userId)) {
            message.setStatus(3);
            message.setUpdateTime(LocalDateTime.now());
            chatMessageMapper.updateById(message);
            
            MessageVO vo = toVO(message);
            webSocketService.pushToUser(message.getReceiverId(), vo);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteMessage(String messageId, Long userId) {
        ChatMessage message = chatMessageMapper.selectById(Long.parseLong(messageId));
        if (message != null && (message.getSenderId().equals(userId) || message.getReceiverId().equals(userId))) {
            message.setDeleted(1);
            message.setUpdateTime(LocalDateTime.now());
            chatMessageMapper.updateById(message);
            log.info("消息软删除成功：messageId={}, userId={}", messageId, userId);
            return true;
        }
        return false;
    }

    @Override
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getReceiverId, userId)
               .eq(ChatMessage::getStatus, STATUS_SENT)
               .eq(ChatMessage::getDeleted, NOT_DELETED);
        return chatMessageMapper.selectCount(wrapper);
    }

    @Override
    public long getSessionUnreadCount(String sessionId, Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .eq(ChatMessage::getReceiverId, userId)
               .eq(ChatMessage::getStatus, STATUS_SENT)
               .eq(ChatMessage::getDeleted, NOT_DELETED);
        return chatMessageMapper.selectCount(wrapper);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getReceiverId, userId)
               .eq(ChatMessage::getStatus, STATUS_SENT)
               .eq(ChatMessage::getDeleted, NOT_DELETED);

        List<ChatMessage> unreadMessages = chatMessageMapper.selectList(wrapper);
        for (ChatMessage message : unreadMessages) {
            message.setStatus(STATUS_READ);
            message.setUpdateTime(LocalDateTime.now());
            chatMessageMapper.updateById(message);
        }
        log.info("标记所有消息已读：userId={}, count={}", userId, unreadMessages.size());
    }

    @Override
    public void pushToQueue(MessageVO message) {
        try {
            webSocketService.pushToUser(message.getReceiverId(), message);
            log.info("消息已推送到队列: messageId={}", message.getId());
        } catch (Exception e) {
            log.error("消息队列推送失败", e);
        }
    }

    @Override
    public void consumeMessage(MessageVO message) {
        log.info("消费消息: messageId={}, senderId={}, receiverId={}", 
                 message.getId(), message.getSenderId(), message.getReceiverId());
    }

    @SuppressWarnings("nullness")
    private MessageVO toVO(ChatMessage message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
