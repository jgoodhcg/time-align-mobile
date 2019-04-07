# Time Align Mobile

## Development && Deployment
Install dependencies
```bash
$ yarn
```
Start shadow
```bash
$ shadow-cljs watch app
```
Wait for first compile to finish or expo gets confused
In another command line start expo
```bash
$ expo start --dev
```
Open the app in the expo client
Close and re-open until the successful repl init message appears in the expo logs
```bash
WebSocket connected!
REPL init successful
```
Open the [developer menu](https://docs.expo.io/versions/latest/workflow/development-mode/#showing-the-developer-menu) and make sure *live reload* and *hot reload* are disabled.

In emacs editor
```
cider-connect (localhost:8202 or w/e the shadow log says the nrepl is running on)
```
Once that clj repl is open then connect to a cljs repl with
```
cider-connect-sibling-cljs
# for the prompts select
shadow
# build
app
```


## production build
```bash
# $ shadow-cljs release app # doesn't work right now
$ shadpow-cljs build app # this means we are publishing the
$ expo build:android and expo-publish after cancelling the watch command
```
