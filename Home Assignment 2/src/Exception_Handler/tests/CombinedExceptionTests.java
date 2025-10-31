package Exception_Handler.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    MultipleExceptionsHandlerTest.class,
    RethrowExceptionHandlerTest.class
})
public class CombinedExceptionTests {}
