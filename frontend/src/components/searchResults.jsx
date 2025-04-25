import { ChevronRight } from "lucide-react";
import Image from "next/image";
import { twMerge } from "tailwind-merge";

// Helper to get favicon URL from a website URL
const getFaviconUrl = (siteUrl) => {
  try {
    const { origin } = new URL(siteUrl);
    return `${origin}/favicon.ico`;
  } catch {
    return "/vercel.svg";
  }
};

export default function SearchResults({ data, ...props }) {
  // Format URL as breadcrumb pieces
  const formatBreadcrumbParts = (url) => {
    try {
      const urlObj = new URL(url);
      const domain = urlObj.hostname;
      const path = urlObj.pathname.split("/").filter(Boolean);
      const queryParams = urlObj.search
        ? urlObj.search
          .substring(1)
          .split("&")
          .map((param) => param.replace("=", ": "))
        : [];

      const parts = [domain, ...path, ...queryParams];
      return parts;
    } catch (e) {
      return [url]; // Return original URL if parsing fails
    }
  };

  return (
    <div className={twMerge("flex flex-col gap-1", props.className)}>
      {/* Search statistics */}
      {props.stats && (
        <div className="text-sm text-gray-400 mb-2">
          {props.stats.resultCount * (props.stats.pages || 1)} results
          {typeof props.stats.elapsedMs === "number" &&
            ` (${props.stats.elapsedMs.toFixed(2)} ms)`}
        </div>
      )}
      {data.map((result, index) => (
        <div key={index} className="flex flex-col">
          {/* URL and favicon */}
          <div className="flex items-center mb-1">
            <div className="flex items-center">
              <div className="flex  rounded-full w-9 h-9 justify-center items-center overflow-hidden mr-2">
                {/* Use <img> instead of <Image> for external favicons to avoid Next.js domain restrictions */}
                <img
                  src={getFaviconUrl(result.url)}
                  alt={result.docTitle}
                  width={20}
                  height={20}
                  style={{ objectFit: "contain" }}
                  onError={(e) => {
                    e.target.src = "/vercel.svg";
                  }}
                />
              </div>

              <div className="flex flex-col">
                <div className="text-md md:text-lg text-white">
                  <span className="font-semibold">
                    {/* Show only the website name (second-level domain) from the URL */}
                    {(() => {
                      try {
                        const hostname = new URL(result.url).hostname;
                        const parts = hostname.split(".");
                        if (parts.length >= 2) {
                          return parts[parts.length - 2];
                        }
                        return hostname;
                      } catch {
                        return result.url;
                      }
                    })()}
                  </span>
                </div>
                <div className="text-[0.8rem] md:text-[1em] text-gray-400 flex items-center overflow-hidden">
                  {formatBreadcrumbParts(result.url)
                    .slice(0, 2)
                    .map((part, idx, arr) => (
                      <span key={idx} className="flex items-center">
                        {part}
                        {idx < arr.length - 1 && (
                          <ChevronRight
                            size={10}
                            className={"text-gray-400 mx-2"}
                          />
                        )}
                      </span>
                    ))}
                </div>
              </div>
            </div>
          </div>

          {/* Title */}
          <a
            href={result.url}
            className="text-xl text-[#75acff] font-medium hover:underline mb-1 leading-tight"
          >
            {result.docTitle}
          </a>

          {/* Description (not available in backend, so leave empty or fallback) */}
          {/* <div className="text-md text-white leading-snug max-w-[60vw]">
            {result.description}
          </div> */}
        </div>
      ))}
    </div>
  );
}
