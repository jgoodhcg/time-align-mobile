# Time Align Mobile

## Development
Install dependencies.
```bash
yarn
```

Start shadow.
```bash
shadow-cljs watch app
```

Wait for first compile to finish or expo gets confused.  
In another process start expo.
```bash
expo start
```

Open the app in the expo client and look for successful repl message.
```bash
WebSocket connected!
REPL init successful
```

Open the [developer menu](https://docs.expo.io/versions/latest/workflow/development-mode/#showing-the-developer-menu) and make sure *live reload* and *hot reload* are disabled.  

In editor connect to the shadow repl. Emacs specific command below.
```
cider-connect (localhost:8202 or w/e the shadow log says the nrepl is running on)
```

Once that clj repl is open then start a cljs repl.  
It should connect to the instance running on the device.  
Emacs specific commands bellow.
```
cider-connect-sibling-cljs
# for the prompts select
shadow
# build
app
```

If the expo client displays an error `dismiss` it and do *not* _reload_.  
Reloading kills the repl. The shadow watch, and expo start commands will have to be restarted and the editor re-connected to the repl.

## Production Build
```bash
shadow-cljs release app
expo build:android
```

## Production Publish
```bash
shadow-cljs release app
expo publish
```

