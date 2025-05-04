import {ChevronRight} from "lucide-react";
import Image from "next/image";
import {twMerge} from "tailwind-merge";

// Helper to get favicon URL from a website URL
const getFaviconUrl = (siteUrl) => {
    try {
        const {origin} = new URL(siteUrl);
        return `${origin}/favicon.ico`;
    } catch {
        return "/vercel.svg";
    }
};

// Helper to highlight query words in the snippet
function highlightSnippet(snippet, query) {
    if (!snippet || !query) return snippet;
    // Split query into words, escape regex
    const words = query
        .split(/\s+/)
        .filter(Boolean)
        .map((w) => w.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
    if (words.length === 0) return snippet;
    const regex = new RegExp(`(${words.join("|")})`, "gi");
    // Replace with <mark> or <b>
    return snippet.replace(regex, "<b>$1</b>");
}

export default function SearchResults({data, ...props}) {
    // Format URL as breadcrumb pieces
    const formatBreadcrumbParts = (url) => {
        try {
            const urlObj = new URL(url);
            const domain = urlObj.hostname;
            const path = urlObj.pathname.split("/").filter(Boolean);
            const queryParams = urlObj.search ? urlObj.search
                .substring(1)
                .split("&")
                .map((param) => param.replace("=", ": ")) : [];

            const parts = [domain, ...path, ...queryParams];
            return parts;
        } catch (e) {
            return [url]; // Return original URL if parsing fails
        }
    };

    // Get query from URL for highlighting
    const query = typeof window !== "undefined" ? new URLSearchParams(window.location.search).get("q") || "" : "";

    return (<div className={twMerge("flex flex-col gap-1", props.className)}>
        {/* Search statistics */}
        {props.stats && (<div className="text-sm text-gray-400 mb-2">
            {props.stats.resultCount * (props.stats.pages || 1)} results
            {typeof props.stats.elapsedMs === "number" && ` (${props.stats.elapsedMs.toFixed(2)} ms)`}
        </div>)}
        {data.map((result, index) => (<div key={index} className="flex flex-col">
            {/* URL and favicon */}
            <div className="flex items-center text-sm text-gray-400 mb-1">
                <Image
                    src={getFaviconUrl(result.url)}
                    alt="favicon"
                    width={16}
                    height={16}
                    className="mr-2"
                />
                {formatBreadcrumbParts(result.url).map((part, i) => (<span key={i} className="flex items-center">
                {i > 0 && <ChevronRight className="w-4 h-4 mx-1" />}
                    {part}
              </span>))}
            </div>
            {/* Title */}
            <a
                href={result.url}
                className="text-xl text-[#75acff] font-medium hover:underline mb-1 leading-tight"
            >
                {result.docTitle}
            </a>

            {/* Description/snippet with query words in bold */}
            {result.snippet && (<div
                className="text-md text-white leading-snug max-w-[60vw] mt-1"
                dangerouslySetInnerHTML={{
                    __html: highlightSnippet(result.snippet, query),
                }}
            />)}
        </div>))}
    </div>);
}