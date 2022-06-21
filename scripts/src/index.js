import { CookieCategoryType, CookieEngine } from 'cookie-cutter';

const MESSAGE_STARTED = 'started';
const MESSAGE_PROVIDER_LOG = 'provider_log';
const MESSAGE_GET_IS_FLAGGED = 'is_flagged';
const MESSAGE_NOTICE_HANDLED = 'notice_handled';
const MESSAGE_GET_PREFERENCES = 'get_preferences';
let preferences = null;

// sends a message to the app
function sendMessage(type, data) {
    const message = { type, data };
    __neeva_broker.postMessage(JSON.stringify(message));
}

// returns a Promise that waits for a response from the app
function waitForData(messageType, params) {
    return new Promise((resolve) => {
        // first prepare a listener for when we get the data
        const handler = (e) => {
            const message = JSON.parse(e.data);
            if (message.type !== messageType) {
                return;
            }
            __neeva_broker.removeEventListener('message', handler);
            resolve(message.data);
        };
        __neeva_broker.addEventListener('message', handler);

        // then ask for it
        sendMessage(messageType, params);
    });
}

// setup
// TODO: site flagging
CookieEngine.flagSite(async () => {});

// Stats aren't used on Android, we can leave this as a no-op
CookieEngine.incrementCookieStats(async () => {});

// controlling script injection is handled by the app, just leave it at true.
CookieEngine.isCookieConsentingEnabled(async () => true);
CookieEngine.isFlaggedSite(() => waitForData(MESSAGE_GET_IS_FLAGGED));
CookieEngine.notifyNoticeHandledOnPage(() =>
    sendMessage(MESSAGE_NOTICE_HANDLED)
);
CookieEngine.getHostname(() => window.location.hostname);

// prefs
CookieEngine.areAllEnabled(async () =>
    Object.keys(preferences).every((key) => preferences[key])
);

CookieEngine.isTypeEnabled(async (type) => {
    switch (type) {
        case CookieCategoryType.Marketing:
        case CookieCategoryType.DoNotSell:
            return preferences.marketing;
        case CookieCategoryType.Analytics:
        case CookieCategoryType.Preferences:
            return preferences.analytics;
        case CookieCategoryType.Social:
        case CookieCategoryType.Unknown:
            return preferences.social;
        case CookieCategoryType.Essential:
            return true;
        default:
            return false;
    }
});

// stats
CookieEngine.logProviderUsage((provider) =>
    sendMessage(MESSAGE_PROVIDER_LOG, provider)
);

// as it turns out, the script is injected long before the document has finished loading
// wait for that, or else we'll be just wasting resources
document.addEventListener('DOMContentLoaded', () => {
    // load preferences
    waitForData(MESSAGE_GET_PREFERENCES).then((prefs) => {
        // then run
        preferences = prefs;
        CookieEngine.runCookieCutter();
        sendMessage(MESSAGE_STARTED);
    });
});
