package bbmovie.ai_platform.agentic_ai;

import org.springframework.ai.chat.client.ChatClient;
import java.lang.reflect.Method;

public class TestReflect {
    public static void main(String[] args) {
        System.out.println(System.getProperty("user.timezone"));
        System.out.println(java.util.TimeZone.getDefault().getID());
        System.out.println("BUILDER METHODS:");
        for (Method m : ChatClient.Builder.class.getMethods()) {
            System.out.println(m.getName() + ": " + java.util.Arrays.toString(m.getParameterTypes()));
        }
        System.out.println("REQUEST SPEC METHODS:");
        for (Method m : ChatClient.ChatClientRequestSpec.class.getMethods()) {
            System.out.println(m.getName() + ": " + java.util.Arrays.toString(m.getParameterTypes()));
        }
    }
}
