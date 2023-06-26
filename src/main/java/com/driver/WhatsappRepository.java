package com.driver;

import java.util.*;

import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;

    private HashMap<String,User> userDB;
    private HashMap<Integer,String> messageDB;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 1;
        this.messageId = 1;
        this.userDB=new HashMap<>();
        this.messageDB=new HashMap<>();
    }

    public boolean createUser(String name,String mobile) {
        if (userMobile.contains(mobile)) return false;
        userMobile.add(mobile);
        userDB.put(mobile,new User(name,mobile));
        return true;
    }

    public Group createGroup(List<User> users) {
        if (users.size()==2){
            Group group = new Group(users.get(1).getName(),2);
            this.groupUserMap.put(group,users);

            adminMap.put(group,users.get(0));

            return group;
        }

        String name="Group "+this.customGroupCount;
        Group group = new Group(name,users.size());
        this.groupUserMap.put(group,users);

        adminMap.put(group,users.get(0));
        this.customGroupCount++;

        return group;
    }

    public int createMessage(String content) {
        Message message=new Message(messageId,content);
        messageId++;
        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception {
        if (!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");
        if (!groupUserMap.get(group).contains(sender)) throw new Exception("You are not allowed to send message");
        senderMap.put(message,sender);
        List<Message> msg = groupMessageMap.getOrDefault(group,new ArrayList<>());
        msg.add(message);
        groupMessageMap.put(group,msg);
        return msg.size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        if (!groupUserMap.containsKey(group)) throw new Exception("Group does not exist");
        if (!adminMap.get(group).getName().equals(approver.getName())) throw new Exception("Approver does not have rights");
        if (!groupUserMap.get(group).contains(user)) throw new Exception("User is not a participant");
        adminMap.put(group,user);
        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception{

        for (Group group:groupUserMap.keySet()) {
            if (groupUserMap.get(group).contains(user)){
                if (adminMap.get(group).equals(user)) throw new Exception("Cannot remove admin");

                groupUserMap.get(group).remove(user);
                List<Message> messages= new ArrayList<>();
                for (Message message:groupMessageMap.get(group)) {
                    if(senderMap.get(message).equals(user)){
                        senderMap.remove(message);
                    }else messages.add(message);
                }
                groupMessageMap.put(group,messages);
                return groupUserMap.get(group).size()+messages.size()+senderMap.size();
            }
        }
        throw new Exception("User not found");
    }

    public String findMessage(Date start, Date end, int k) throws Exception {
        List<Message> messages=new ArrayList<>();
        for (Message msg:senderMap.keySet()) {
            if (start.before(msg.getTimestamp()) && end.after(msg.getTimestamp())) messages.add(msg);
        }

        if (k>messages.size()) throw new Exception("K is greater than the number of messages");

        Collections.sort(messages,(a,b)->{
            if(a.getTimestamp().before(b.getTimestamp())) return -1;
            return 1;
        });

        return messages.get(messages.size()-k).toString();
    }
}
