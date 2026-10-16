// Page side of the Mindustry browser build: loads the natives, the asset packs and the
// player's files, records input for WebInput.java, and keeps the files the game writes for the page.
(function(){
    "use strict";

    /** Reads a pak's header: the files it lists and where their bytes start. */
    function readHeader(buf, length){
        if(length < 4) return null;
        const headLen = new DataView(buf.buffer, buf.byteOffset).getUint32(0, true);
        if(length < 4 + headLen) return null;
        return {list: JSON.parse(new TextDecoder().decode(buf.subarray(4, 4 + headLen))), start: 4 + headLen};
    }

    /** The files in a whole pak; lazy ones are listed with no bytes (the music pak has them). */
    function readPack(buf){
        const {list, start} = readHeader(buf, buf.length);
        const files = [];
        let off = start;
        for(const [path, size, lazy] of list){
            if(lazy){
                files.push({path, data: new Int8Array(0)});
                continue;
            }
            files.push({path, data: new Int8Array(buf.buffer, buf.byteOffset + off, size)});
            off += size;
        }
        return files;
    }

    /**
     * Downloads a pak and hands over each file as soon as its bytes are in.
     * Calls done(false) when the download fails part way.
     */
    async function streamPack(url, onFile, done){
        try{
            const res = await fetch(url);
            if(!res.ok) throw new Error(url + ": " + res.status);
            const reader = res.body.getReader();
            let buf = new Uint8Array(1 << 16), length = 0, header = null, next = 0, off = 0;
            for(;;){
                const {done: end, value} = await reader.read();
                if(end) break;
                if(length + value.length > buf.length){
                    const grown = new Uint8Array(Math.max(buf.length * 2, length + value.length));
                    grown.set(buf.subarray(0, length));
                    buf = grown;
                }
                buf.set(value, length);
                length += value.length;
                if(!header && (header = readHeader(buf, length))) off = header.start;
                while(header && next < header.list.length && off + header.list[next][1] <= length){
                    const [path, size] = header.list[next++];
                    onFile(path, new Int8Array(buf.slice(off, off + size).buffer));
                    off += size;
                }
            }
            done(!!header && next === header.list.length);
        }catch(e){
            done(false);
        }
    }

    function loadScript(src){
        return new Promise((resolve, reject) => {
            const s = document.createElement("script");
            s.src = src;
            s.onload = resolve;
            s.onerror = () => reject(new Error("Could not load " + src));
            document.head.appendChild(s);
        });
    }

    // keys the browser keeps: reload, full screen (the page's), developer tools
    const keep = new Set(["F5", "F11", "F12"]);
    const modifiers = new Set(["ControlLeft", "ControlRight", "MetaLeft", "MetaRight"]);
    // with Ctrl or Meta held, only the game's own shortcuts; the rest stay the browser's
    const shortcuts = new Set(["KeyA", "KeyC", "KeyX", "KeyZ", "KeyY"]);

    function recordInput(host, canvas){
        const ev = host.events;
        const pos = (e) => {
            const r = canvas.getBoundingClientRect();
            return {x: e.clientX - r.left, y: e.clientY - r.top};
        };
        canvas.tabIndex = 0;

        canvas.addEventListener("keydown", (e) => {
            if(keep.has(e.code) || ((e.ctrlKey || e.metaKey) && !modifiers.has(e.code) && !shortcuts.has(e.code))) return;
            e.preventDefault();
            ev.push({t: "kd", code: e.code, repeat: e.repeat});
            if(e.key.length === 1 && !e.ctrlKey && !e.metaKey) ev.push({t: "ch", key: e.key});
        });
        canvas.addEventListener("keyup", (e) => {
            ev.push({t: "ku", code: e.code});
        });
        canvas.addEventListener("paste", (e) => {
            const text = e.clipboardData && e.clipboardData.getData("text");
            if(text) ev.push({t: "ch", key: text});
            e.preventDefault();
        });

        canvas.addEventListener("pointerdown", (e) => {
            canvas.focus({preventScroll: true});
            canvas.setPointerCapture(e.pointerId);
            const p = pos(e);
            if(e.pointerType === "mouse") ev.push({t: "md", x: p.x, y: p.y, b: e.button});
            else ev.push({t: "td", x: p.x, y: p.y, id: e.pointerId});
            e.preventDefault();
        });
        const up = (e) => {
            const p = pos(e);
            if(e.pointerType === "mouse") ev.push({t: "mu", x: p.x, y: p.y, b: e.button});
            else ev.push({t: "tu", x: p.x, y: p.y, id: e.pointerId});
        };
        canvas.addEventListener("pointerup", up);
        canvas.addEventListener("pointercancel", up);
        canvas.addEventListener("pointermove", (e) => {
            const p = pos(e);
            if(e.pointerType === "mouse") ev.push({t: "mm", x: p.x, y: p.y});
            else ev.push({t: "tm", x: p.x, y: p.y, id: e.pointerId});
        });
        canvas.addEventListener("wheel", (e) => {
            e.preventDefault();
            ev.push({t: "wh", x: Math.sign(e.deltaX), y: Math.sign(e.deltaY)});
        }, {passive: false});
        canvas.addEventListener("contextmenu", (e) => e.preventDefault());
        canvas.addEventListener("blur", () => ev.push({t: "blur"}));
        canvas.style.touchAction = "none";

        // on touch screens, text fields need a real input to bring up the keyboard
        const text = document.createElement("input");
        text.type = "text";
        text.autocomplete = "off";
        text.setAttribute("autocapitalize", "off");
        text.setAttribute("aria-hidden", "true");
        text.tabIndex = -1;
        text.style.cssText = "position:fixed;left:0;bottom:0;width:1px;height:1px;opacity:0;pointer-events:none";
        document.body.appendChild(text);
        text.addEventListener("beforeinput", (e) => {
            if(e.inputType === "deleteContentBackward"){
                ev.push({t: "kd", code: "Backspace"}, {t: "ku", code: "Backspace"});
            }else if(e.inputType === "insertLineBreak"){
                ev.push({t: "kd", code: "Enter"}, {t: "ku", code: "Enter"});
            }else if(e.data){
                ev.push({t: "ch", key: e.data});
            }
            e.preventDefault();
        });
        let touched = false;
        canvas.addEventListener("pointerdown", (e) => { touched = e.pointerType !== "mouse"; });
        host.textInput = (on) => {
            if(on && touched) text.focus({preventScroll: true});
            else if(!on && document.activeElement === text) canvas.focus({preventScroll: true});
        };
    }

    window.startMindustry = async function(opts){
        const canvas = opts.canvas;
        const base = opts.base;
        // the player's files, kept by the page: path -> {data, modified}
        const files = new Map();
        for(const f of opts.saved || []) files.set(f.path, {data: f.data, modified: f.modified || Date.now()});
        const saved = [...files].map(([path, f]) => ({path, data: new Int8Array(f.data.buffer, f.data.byteOffset, f.data.length), modified: f.modified}));

        const [natives] = await Promise.all([
            loadScript(base + "arc-natives.js").then(() => window.createArcNatives({locateFile: (f) => base + f})),
            loadScript(base + "mindustry.js")
        ]);
        const packs = opts.packs.map(readPack);
        window.arcNatives = natives;
        // SoLoud starts with the game (its init sets the volume), so this is applied again once it runs
        let muted = !!opts.muted;

        let version = 0;
        // music: what came in so far, who waits for what, and whether the download is over
        const music = new Map(), waiting = new Map();
        let musicDone = false;

        const host = window.mindustryHost = {
            canvas,
            events: [],
            assets: [].concat(...packs),
            saved,
            files,
            /** Goes up each time a file changes. */
            version: () => version,
            store(path, data, modified){
                files.set(path, {data: new Uint8Array(data.buffer.slice(data.byteOffset, data.byteOffset + data.length)), modified});
                version++;
            },
            remove(path){
                if(files.delete(path)) version++;
            },
            setMuted(m){
                muted = m;
                natives._arc_set_muted(m ? 1 : 0);
            },
            onVisibility: null,
            writeClipboard(text){
                if(navigator.clipboard) navigator.clipboard.writeText(text).catch(() => {});
            },
            download(name, data){
                const a = document.createElement("a");
                a.href = URL.createObjectURL(new Blob([data]));
                a.download = name;
                a.click();
                setTimeout(() => URL.revokeObjectURL(a.href), 10000);
            },
            pickFile(accept, done){
                const input = document.createElement("input");
                input.type = "file";
                input.accept = accept;
                input.addEventListener("change", async () => {
                    const f = input.files[0];
                    if(f) done(f.name, new Int8Array(await f.arrayBuffer()));
                });
                input.click();
            },
            openURI(uri){
                window.open(uri, "_blank", "noopener");
            },
            cursorUrl(pixels, w, h){
                const c = document.createElement("canvas");
                c.width = w;
                c.height = h;
                c.getContext("2d").putImageData(new ImageData(new Uint8ClampedArray(pixels), w, h), 0, 0);
                return c.toDataURL();
            },
            textInput(){},
            crash(message){
                if(opts.onCrash) opts.onCrash(message);
                else console.error(message);
            },
            log(message){
                if(opts.onLog) opts.onLog(message);
            },
            exit(){
                if(opts.onExit) opts.onExit();
            },
            ready(){
                natives._arc_set_muted(muted ? 1 : 0);
                // after the start, so it doesn't hold up anything the game waits for;
                // a download that fails is tried again a little later, twice
                const fetchMusic = (attempt) => streamPack(opts.music, (path, data) => {
                    if(music.has(path)) return;
                    music.set(path, data);
                    for(const id of waiting.get(path) || []) host.lazyLoaded.push({id, data});
                    waiting.delete(path);
                }, (ok) => {
                    if(!ok && attempt < 3) return setTimeout(() => fetchMusic(attempt + 1), attempt * 5000);
                    musicDone = true;
                    for(const ids of waiting.values()) for(const id of ids) host.lazyLoaded.push({id, data: null});
                    waiting.clear();
                });
                if(opts.music) fetchMusic(1);
                else musicDone = true;
                if(opts.onReady) opts.onReady();
            },
            lazyLoaded: [],
            fetchLazy(id, path){
                if(music.has(path) || musicDone) host.lazyLoaded.push({id, data: music.get(path) || null});
                else waiting.set(path, (waiting.get(path) || []).concat(id));
            }
        };
        recordInput(host, canvas);
        document.addEventListener("visibilitychange", () => {
            if(host.onVisibility) host.onVisibility(!document.hidden);
        });
        window.main([]);
        return host;
    };
})();
