## MPS vs IDEA environment

Since plugin version 1.10, the `environmentKind` parameter allows to choose between MPS or IDEA environment for
generation or model check. The default is to use the IDEA environment because that was the case in earlier versions, but
the MPS environment is lighter and likely to be more performant. On the other hand, the MPS environment does not load
plugins (only extensions) and may lead to different results.
