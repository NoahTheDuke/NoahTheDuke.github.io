// taken from https://github.com/gohjy/bsky-to-aturi

async function resolveHandle(handle) {
    const fetchUrl = new URL("https://public.api.bsky.app/xrpc/com.atproto.identity.resolveHandle");
    fetchUrl.searchParams.set("handle", handle);
    const fetchResult = await fetch(fetchUrl.href).catch(() => ({
        error: "FetchError",
        message: "Failed to fetch Bluesky API"
    }));
    if (fetchResult.error) return fetchResult;

    const json = await fetchResult.json().catch(() => ({
        error: "ServerError",
        message: "Bluesky API did not return valid JSON"
    }));

    if (json.did) return json.did;
    return json;
}

const REGEX = {
    // https://atproto.com/specs/did#at-protocol-did-identifier-syntax
    DID: /^(did:[a-z]+:[a-zA-Z0-9._:%-]*)\/app.bsky.feed.post\/([a-zA-Z0-9._-]*)$/,

    // https://atproto.com/specs/handle#handle-identifier-syntax
    HANDLE: /^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$/,

    // https://github.com/bluesky-social/atproto/blob/main/lexicons/app/bsky/feed/post.json#L8 specifies app.bsky.feed.post records must have TID rkey
    // https://atproto.com/specs/tid#tid-syntax
    TID: /^[234567abcdefghij][234567abcdefghijklmnopqrstuvwxyz]{12}$/
};

function destructureBskyUrl(bskyUrl) {
    // "at://did:plc:qbidoe2gpkfelnurqod37ikr/app.bsky.feed.post/3mlu74wihik27"
    const urlObj = (() => {
        try {
            return new URL(bskyUrl);
        } catch {
            if (bskyUrl.startsWith("at://"))
                return {pathname: bskyUrl.replace("at://", "")}

            return null;
        }
    })();
    if (!urlObj) return null;

    const pathname = urlObj.pathname;
    console.log(pathname);

    const [_didMatch, repo, rkey] = pathname.match(REGEX.DID) || [];
    if (_didMatch) {
        return {
            type: "did",
            repo,
            rkey
        };
    }

    const [_fullMatch, handleOrDid, rkey2] = pathname.match(/^\/profile\/([^\/]+)(?:\/(?:post\/([^\/]+)\/?)?)?$/) || [null];

    if (!_fullMatch) return null;
    if (rkey && !rkey.match(REGEX.TID)) return null;

    if (handleOrDid.match(REGEX.HANDLE)) {
        return {
            type: "handle", 
            repo: handleOrDid,
            rkey: rkey2,
        };
    } else if (handleOrDid.match(REGEX.DID)) {
        return {
            type: "did",
            repo: handleOrDid,
            rkey: rkey2,
        };
    }

    return null;
}

function constructUri({repo, rkey}) {
    if (!rkey) {
        // default to app.bsky.actor.profile
        return `at://${repo}/app.bsky.actor.profile/self`;
    }
    return `at://${repo}/app.bsky.feed.post/${rkey}`;
}

async function getUri(bskyUrl) {
    const urlComponents = destructureBskyUrl(bskyUrl);
    if (!urlComponents) throw {
        error: "InvalidUrl",
        message: "Invalid Bluesky URL provided"
    };

    if (urlComponents.type === "did") {
        // just construct URI and return
        return constructUri(urlComponents);
    } else {
        // it's a handle
        // 1. resolve handle
        const did = await resolveHandle(urlComponents.repo);
        if (did.error) throw did;

        // 2. construct URI and return
        return constructUri({
            repo: did,
            rkey: urlComponents.rkey
        });
    }
}

// taken from https://capscollective.com/blog/bluesky-blog-comments/

function getPostLinkFromUri(uri) {
    return `https://bsky.app/profile/${uri.replace("at://", "").replace("/app.bsky.feed.post/", "/post/")}`;
}

function getImageLinkFromBlob(did, blobLink, useThumbnail) {
    return `https://cdn.bsky.app/img/${useThumbnail ? "feed_thumbnail" : "feed_fullsize"}/plain/${did}/${blobLink}`;
}

function createHtmlTextForRecord(record) {
    var htmlText = `<span>`

    const textEncoder = new TextEncoder();
    const utf8Decoder = new TextDecoder();
    const utf8Text = new Uint8Array(record.text.length * 3);
    textEncoder.encodeInto(record.text, utf8Text);

    var charIdx = 0;
    for (const facetIdx in record.facets) {
        const facet = record.facets[facetIdx];
        const facetFeature = facet.features[0];
        const facetType = facetFeature.$type;

        var facetLink = "#";
        if (facetType == "app.bsky.richtext.facet#tag") {
            facetLink = `https://bsky.app/hashtag/${facetFeature.tag}`;
        }
        else if (facetType == "app.bsky.richtext.facet#link") {
            facetLink = facetFeature.uri;
        }
        else if (facetType == "app.bsky.richtext.facet#mention") {
            facetLink = `https://bsky.app/profile/${facetFeature.did}`;
        }

        if (charIdx < facet.index.byteStart) {
            const preFacetText = utf8Text.slice(charIdx, facet.index.byteStart);
            htmlText += utf8Decoder.decode(preFacetText)
        }

        const facetText = utf8Text.slice(facet.index.byteStart, facet.index.byteEnd);
        htmlText += `<a class="comment-facet-link" href="${facetLink}" target="_blank">` + utf8Decoder.decode(facetText) + '</a>';

        charIdx = facet.index.byteEnd;
    }

    if (charIdx < utf8Text.length) {
        const postFacetText = utf8Text.slice(charIdx, utf8Text.length);
        htmlText += utf8Decoder.decode(postFacetText);
    }

    return htmlText + '</span>';
}

function createHtmlForReplies(replies) {
    var htmlText = ""
    for (const replyIdx in replies) {
        const reply = replies[replyIdx];
        const replyPost = reply.post;
        const record = replyPost.record;
        const postLink = getPostLinkFromUri(replyPost.uri);
        const postTimestamp = record.createdAt;
        const postText = createHtmlTextForRecord(record);

        var embedHtml = "";
        if (replyPost.embed) {
            if (replyPost.embed.$type == "app.bsky.embed.external#view") {
                const postEmbedExternal = replyPost.embed.external;
                if (postEmbedExternal.uri.includes(".gif?")) {
                    embedHtml += `<img class="comment-image" src="${postEmbedExternal.uri}" title="${postEmbedExternal.title}" alt="${postEmbedExternal.description}">`
                }
            }
            else if (replyPost.embed.$type == "app.bsky.embed.images#view") {
                const images = replyPost.record.embed.images;
                for (const imageIdx in images) {
                    const image = images[imageIdx];
                    const thumbnailLink = getImageLinkFromBlob(replyPost.author.did, image.image.ref.$link, true);
                    const fullSizelLink = getImageLinkFromBlob(replyPost.author.did, image.image.ref.$link, false);
                    embedHtml += `<a href="${fullSizelLink}" target="_blank"><img class="comment-image" src="${thumbnailLink}" alt="${image.alt}"></a>`
                }
            }
        }

        htmlText += `
            <div class="comment-block">
            <span class="comment-title"><img src="${replyPost.author.avatar}" class="comment-avatar"> ${replyPost.author.displayName} <a href="https://bsky.app/profile/${replyPost.author.did}" target="_blank">@${replyPost.author.handle}</a></span><span class="comment-text"> - ${(new Date(postTimestamp)).toLocaleString()}</span>
            <div class="comment-text">${postText}</div>
            ${embedHtml}
            <div class="comment-links">
                <span><a class="comment-link comment-reply-link" href="${postLink}" target="_blank">${replyPost.replyCount} repl${replyPost.replyCount !== 1 ? "ies" : "y"}</a></span>
            </div>
            <div class="comment-reply">
            ${createHtmlForReplies(reply.replies)}
            </div>
            </div>`
    }
    return htmlText;
}

function httpGetAsJsonAsync(url, callback) {
    var xmlHttp = new XMLHttpRequest();
    getUri(url).then(uri => {
        const bskyUrl = "https://public.api.bsky.app/xrpc/app.bsky.feed.getPostThread?uri=" + uri;
        console.log(bskyUrl);
        xmlHttp.onreadystatechange = function() { 
            if (xmlHttp.readyState == 4 && xmlHttp.status == 200) {
                var responseJson = JSON.parse(xmlHttp.responseText);
                if (typeof(responseJson) == 'undefined') {
                    return;
                }
                var element = document.getElementById("show-comments");
                element.parentNode.removeChild(element);
                callback(responseJson);
            }
        }
        xmlHttp.open("GET", bskyUrl, true);
        xmlHttp.send(null);
    });
}

function responseCallback(responseJson) {
    const thread = responseJson["thread"];
    const threadPost = thread["post"];
    const postLink = getPostLinkFromUri(threadPost.uri);

    const threadReplies = thread.replies;
    document.getElementById("social-section").innerHTML = `
        <h4><b>Comments</b></h4>
        <p class="comment-text"><i>Reply on Bluesky <a href="${postLink}" target="_blank">here</a> to join the conversation.</i></p>
        <div class="comment-section">
        ${threadReplies.length > 0 ? createHtmlForReplies(threadReplies) : '<p class="comment-text">No comments to display</p>'}
        </div>`
}
