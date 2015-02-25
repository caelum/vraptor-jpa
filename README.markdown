## vraptor-jpa

![Build status](https://secure.travis-ci.org/caelum/vraptor-jpa.png)

A VRaptor JPA plugin to use with VRaptor 4 with environment support.

# Installing

[Copy the jar file](http://repo1.maven.org/maven2/br/com/caelum/vraptor/vraptor-jpa/) to your app or use:

```xml
<dependency>
	<groupId>br.com.caelum.vraptor</groupId>
	<artifactId>vraptor-jpa</artifactId>
	<version>4.0.2</version> <!-- or the latest version -->
</dependency>
```

This plugin doesn't include any JPA Provider. You need to add your prefered provider.

# Transactional Control

The default behavior is that each request will have a transaction available.

If you want, you can enable a decorator to change this behavior. 

When enabled, it will open transactions only for methods with `@Transactional` annotation. 

To do that you just need to add the follow content into your project's `beans.xml`:

```xml
<decorators>
    <class>br.com.caelum.vraptor.jpa.TransactionDecorator</class>
</decorators>
```

# CDI Events

While the JPATransactionInterceptor worries about handling the transaction, you can observe CDI events to include some logic of yours.

**Please, note that if you specializes or override the JPATransactionInterceptor, those events won't be fired.**

*Remember that CDI Events doesn't have an order when executing observers, so, when observing the same event in more than one method, they should be independents.*

The events are:
* Before trying to commit the transaction: `BeforeCommit`;
* After successfully committing the transaction: `AfterCommit`;
* After successfully rolling back (rollback) the transaction: `AfterRollback`;

```Java
import javax.enterprise.event.Observes;

import br.com.caelum.vraptor.jpa.event.BeforeCommit;
import br.com.caelum.vraptor.jpa.event.AfterCommit;
import br.com.caelum.vraptor.jpa.event.AfterRollback;

public class JPATransactionEventsObserver {
	/* You can @Inject any dependencies here. */

	public void beforeCommit(@Observes BeforeCommit before) {/* ... */}

	public void afterCommit(@Observes AfterCommit after) {/* ... */}

	public void afterRollback(@Observes AfterRollback after) {/* ... */}
}
```

# Help

Get help from vraptor developers and the community at VRaptor's mailing list.
