import {useSearchParams} from "next/navigation";
import {ChevronLeft, ChevronRight, Loader2} from "lucide-react";
import Link from "next/link";
import { useState, useEffect } from "react";

export function Pagination({pagesNum}) {
    const searchParams = useSearchParams();
    const currentPage = parseInt(searchParams.get("page") > 0 ? searchParams.get("page") : "1", 10);
    const [loadingPage, setLoadingPage] = useState(null);

    // Reset loading state when search parameters change
    useEffect(() => {
        setLoadingPage(null);
    }, [searchParams]);

    // Maximum number of page buttons to show
    const maxPageButtons = 7;

    // Logic to determine which pages to show
    let startPage, endPage;

    if (pagesNum <= maxPageButtons) {
        // If total pages is less than our maximum, show all pages
        startPage = 1;
        endPage = pagesNum;
    } else {
        // Calculate middle pages with ellipsis
        const leftSiblingIndex = Math.max(currentPage - 1, 1);
        const rightSiblingIndex = Math.min(currentPage + 1, pagesNum);

        // Should we show left dots
        const shouldShowLeftDots = leftSiblingIndex > 2;
        // Should we show right dots
        const shouldShowRightDots = rightSiblingIndex < pagesNum - 1;

        if (!shouldShowLeftDots && shouldShowRightDots) {
            // Show first pages without left dots
            startPage = 1;
            endPage = Math.min(1 + maxPageButtons - 2, pagesNum);
        } else if (shouldShowLeftDots && !shouldShowRightDots) {
            // Show last pages without right dots
            endPage = pagesNum;
            startPage = Math.max(pagesNum - maxPageButtons + 2, 1);
        } else if (shouldShowLeftDots && shouldShowRightDots) {
            // Show middle pages with both dots
            startPage = Math.max(currentPage - 1, 1);
            endPage = Math.min(currentPage + 1, pagesNum);

            // Adjust to show more pages if possible
            const pagesToShow = endPage - startPage + 1;
            if (pagesToShow < maxPageButtons - 4) {
                // We have space to show more pages
                if (currentPage < pagesNum / 2) {
                    // Closer to start, show more pages on right
                    endPage = Math.min(endPage + (maxPageButtons - 4 - pagesToShow), pagesNum - 1);
                } else {
                    // Closer to end, show more pages on left
                    startPage = Math.max(startPage - (maxPageButtons - 4 - pagesToShow), 2);
                }
            }
        } else {
            // Show all pages without dots
            startPage = 1;
            endPage = pagesNum;
        }
    }

    // Generate the array of page numbers
    const pages = [];

    // Always include first page
    pages.push(1);

    // Add left ellipsis if needed
    if (startPage > 2) {
        pages.push("ellipsis-left");
    }

    // Add middle pages
    for (let i = Math.max(2, startPage); i <= Math.min(pagesNum - 1, endPage); i++) {
        pages.push(i);
    }

    // Add right ellipsis if needed
    if (endPage < pagesNum - 1) {
        pages.push("ellipsis-right");
    }

    // Always include last page if it's not already included
    if (pagesNum > 1 && !pages.includes(pagesNum)) {
        pages.push(pagesNum);
    }

    // Helper function to create URL for a page
    const createPageUrl = (page) => {
        return `/search?q=${searchParams.get("q")}&page=${page}`;
    };

    // Handle page click with loading state
    const handlePageClick = (page) => {
        if (page !== currentPage) {
            setLoadingPage(page);
        }
    };

    return (
        <div className="flex items-center justify-center space-x-1 py-4">
            {/* Previous Page Button */}
            <Link
                href={currentPage > 1 ? createPageUrl(currentPage - 1) : "#"}
                className={`flex items-center px-3 py-2 rounded-md ${
                    currentPage > 1
                        ? "text-blue-600 hover:bg-blue-100 transition-colors"
                        : "text-gray-300 cursor-not-allowed"
                }`}
                aria-disabled={currentPage <= 1}
                onClick={() => currentPage > 1 && handlePageClick(currentPage - 1)}
            >
                {loadingPage === currentPage - 1 ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                    <ChevronLeft className="h-4 w-4" />
                )}
                <span className="hidden sm:inline ml-1">Previous</span>
            </Link>

            {/* Page Numbers */}
            <div className="hidden sm:flex space-x-1">
                {pages.map((page, index) => {
                    if (page === "ellipsis-left" || page === "ellipsis-right") {
                        return (
                            <span key={page} className="px-3 py-2 text-gray-500">
                                &hellip;
                            </span>
                        );
                    }

                    return (
                        <Link
                            key={page}
                            href={createPageUrl(page)}
                            className={`px-3 py-2 rounded-md flex items-center justify-center min-w-[2.5rem] ${
                                currentPage === page
                                    ? "bg-blue-600 text-white font-medium"
                                    : "text-blue-600 hover:bg-blue-100 transition-colors"
                            }`}
                            aria-current={currentPage === page ? "page" : undefined}
                            onClick={() => handlePageClick(page)}
                        >
                            {loadingPage === page ? (
                                <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                                page
                            )}
                        </Link>
                    );
                })}
            </div>

            {/* Mobile page indicator */}
            <span className="sm:hidden text-gray-700">
                {currentPage} / {pagesNum}
            </span>

            {/* Next Page Button */}
            <Link
                href={currentPage < pagesNum ? createPageUrl(currentPage + 1) : "#"}
                className={`flex items-center px-3 py-2 rounded-md ${
                    currentPage < pagesNum
                        ? "text-blue-600 hover:bg-blue-100 transition-colors"
                        : "text-gray-300 cursor-not-allowed"
                }`}
                aria-disabled={currentPage >= pagesNum}
                onClick={() => currentPage < pagesNum && handlePageClick(currentPage + 1)}
            >
                <span className="hidden sm:inline mr-1">Next</span>
                {loadingPage === currentPage + 1 ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                    <ChevronRight className="h-4 w-4" />
                )}
            </Link>
        </div>
    );
}