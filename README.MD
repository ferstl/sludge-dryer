# sludge-dryer
*- Dries the sludge out of Spring 4.0.x' implementation of CacheAspectSupport#generateKey()*

In version 4.0.x, Spring's `CacheAspectSupport#generateKey()` contains an `Assert.notNull()` statement which creates
tons of garbage by string concatenation. This Java agent replaces this method's implementation with a more efficient one.  

## Usage

    # Build
    mvn clean package
    
    # Run
    java -javaagent:path/to/sludge-dryer.jar MainClass
    
# FAQ
Q: Why don't you just update to Spring 4.1+ which fixes the problem?
A: It's not an option at the moment but that's a whole different story...