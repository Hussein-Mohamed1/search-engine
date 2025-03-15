"use client";

import useSearchStore from "@/store/searchStore";
import { useRouter, useSearchParams } from "next/navigation";
import { twMerge } from "tailwind-merge";

export function SearchButtons() {
  const { query } = useSearchStore();
  const searchParams = useSearchParams();
  const { push } = useRouter();

  const onSearch = () => {
    if (!query) return;

    const params = new URLSearchParams(searchParams);
    if (query) {
      params.set("q", query);
    } else {
      params.delete("q");
    }
    push(`/search?${params.toString()}`);
  };

  return (
    <div
      className={twMerge(
        "font-[inter] font-[400] flex flex-row space-x-4 md:text-2xl text-[1.1rem]"
      )}
    >
      <button
        className="px-3 py-2 rounded-2xl bg-[#1F5673] shadow-[4px_4px_0px_#163E55] hover:shadow-[2px_2px_0px_#163E55] active:shadow-inner active:translate-y-0.5 transition-all"
        onClick={onSearch}
      >
        Search
      </button>

      <button className="px-3 py-2 rounded-2xl bg-[#1F5673] shadow-[4px_4px_0px_#163E55] hover:shadow-[2px_2px_0px_#163E55] active:shadow-inner active:translate-y-0.5 transition-all">
        I&#39;m Feeling Lucky
      </button>
    </div>
  );
}
