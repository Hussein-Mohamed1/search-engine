import { useSearchParams } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Link from "next/link";
export function Pagination({ pagesNum }) {
  const searchParams = useSearchParams();
  const currentPage = parseInt(searchParams.get("page") || "1", 10);

  // Limit displayed pages to 10
  const displayLimit = 10;
  const displayPages = Math.min(pagesNum, displayLimit);

  // Calculate the current group of pages (0-based for calculation purposes)
  // Using Math.floor((currentPage - 1) / displayLimit) instead of Math.floor(currentPage / displayLimit)
  // This ensures page 10 stays in the first group (0-9 range)
  const currentGroup = Math.floor((currentPage - 1) / displayLimit);

  // Calculate start and end page numbers for the current group
  const startPage = currentGroup * displayLimit + 1;
  const endPage = Math.min((currentGroup + 1) * displayLimit, pagesNum);

  // Create pagination letters and numbers
  const paginationItems = [];

  // Add 'L' as first letter
  paginationItems.push(
    <div key="letter-L" className="flex flex-col items-center mx-1">
      <div className="text-blue-500 font-bold text-2xl">L</div>
      {displayPages >= 1 && (
        <Link
          href={`/search?q=${searchParams.get("q")}&page=${startPage}`}
          className={`mt-1 px-2 py-1 rounded-full ${
            currentPage === startPage
              ? "bg-blue-500 text-white font-medium"
              : "text-blue-500 hover:underline"
          }`}
        >
          {startPage}
        </Link>
      )}
    </div>
  );

  // Add 'U's for pages 2 through N-3 (within the current group)
  for (let i = startPage + 1; i <= endPage - 3 || i <= pagesNum - 1; i++) {
    paginationItems.push(
      <div key={`letter-U-${i}`} className="flex flex-col items-center mx-1">
        <div className="text-blue-500 font-bold text-2xl">U</div>
        <Link
          href={`/search?q=${searchParams.get("q")}&page=${i}`}
          className={`mt-1 px-2 py-1 rounded-full ${
            currentPage === i
              ? "bg-blue-500 text-white font-medium"
              : "text-blue-500 hover:underline"
          }`}
        >
          {i}
        </Link>
      </div>
    );
  }

  // Add 'M', 'O', 'S' for the last three pages of the current group
  const lastLetters = ["M", "O", "S"];
  let pageIndex = Math.max(startPage + 2, endPage - 2);

  for (let i = 0; i < 3; i++, pageIndex++) {
    const letter = lastLetters[i];

    paginationItems.push(
      <div
        key={`letter-${letter}`}
        className="flex flex-col items-center mx-1 align-text-top"
      >
        <div className="text-blue-500 font-bold text-2xl align-text-top">
          {letter}
        </div>
        {
          <Link
            href={
              pageIndex <= endPage
                ? `/search?q=${searchParams.get("q")}&page=${pageIndex}`
                : ``
            }
            className={`mt-1 px-2 py-1 rounded-full ${
              currentPage === pageIndex
                ? "bg-blue-500 text-white font-medium cursor-pointer"
                : pageIndex <= endPage
                  ? "text-blue-500 hover:underline cursor-pointer"
                  : "cursor-not-allowed"
            }`}
          >
            {pageIndex <= endPage ? pageIndex : "-"}
          </Link>
        }
      </div>
    );
  }

  return (
    <>
      <div className={`flex flex-row items-center`}>
        {currentPage > 10 && (
          <Link
            href={{
              pathname: "/search",
              query: {
                q: searchParams.get("q"),
                page:
                  1 +
                  displayLimit *
                    Math.floor((currentPage - 1) / displayLimit - 1),
              },
            }}
          >
            <ChevronLeft />
          </Link>
        )}
        {paginationItems}

        {displayLimit * Math.floor((currentPage - 1) / displayLimit + 1) <=
          pagesNum && (
          <Link
            href={{
              pathname: "/search",
              query: {
                q: searchParams.get("q"),
                page:
                  1 +
                  displayLimit *
                    Math.floor((currentPage - 1) / displayLimit + 1),
              },
            }}
          >
            <ChevronRight />
          </Link>
        )}
      </div>
    </>
  );
}
