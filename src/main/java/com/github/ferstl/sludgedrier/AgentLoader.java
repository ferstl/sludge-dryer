package com.github.ferstl.sludgedrier;

import java.io.IOException;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class AgentLoader {

  public static void main(String... args) {
    if (args.length != 2) {
      System.err.println("Usage: java -jar /path/to/sludge-dryer.jar /path/to/sludge-dryer.jar <pid>");
      System.exit(1);
    }

    String agent = args[0];
    String pid = args[1];
    try {
      VirtualMachine vm = VirtualMachine.attach(pid);
      vm.loadAgent(agent);
      vm.detach();
    } catch (AttachNotSupportedException | IOException | AgentLoadException | AgentInitializationException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
